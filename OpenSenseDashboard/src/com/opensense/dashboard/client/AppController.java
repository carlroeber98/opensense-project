package com.opensense.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.opensense.dashboard.client.event.AddSensorsToFavoriteListEvent;
import com.opensense.dashboard.client.event.CloseTourEvent;
import com.opensense.dashboard.client.event.OpenDataPanelPageEvent;
import com.opensense.dashboard.client.event.RemoveSensorsFromFavoriteListEvent;
import com.opensense.dashboard.client.event.StartTourEvent;
import com.opensense.dashboard.client.gui.GUIImageBundle;
import com.opensense.dashboard.client.model.DataPanelPage;
import com.opensense.dashboard.client.model.ParamType;
import com.opensense.dashboard.client.presenter.DataPanelPresenter;
import com.opensense.dashboard.client.presenter.FooterPresenter;
import com.opensense.dashboard.client.presenter.IPresenter;
import com.opensense.dashboard.client.presenter.NavigationPanelPresenter;
import com.opensense.dashboard.client.presenter.UserPresenter;
import com.opensense.dashboard.client.services.AuthenticationService;
import com.opensense.dashboard.client.services.GeneralService;
import com.opensense.dashboard.client.utils.CookieManager;
import com.opensense.dashboard.client.utils.DefaultAsyncCallback;
import com.opensense.dashboard.client.utils.Languages;
import com.opensense.dashboard.client.utils.tourutils.TourBuilder;
import com.opensense.dashboard.client.view.DataPanelViewImpl;
import com.opensense.dashboard.client.view.FooterViewImpl;
import com.opensense.dashboard.client.view.NavigationPanelViewImpl;
import com.opensense.dashboard.shared.ActionResult;
import com.opensense.dashboard.shared.ActionResultType;
import com.opensense.dashboard.shared.Parameter;

import gwt.material.design.client.ui.MaterialToast;

/**
 * This is like the main presenter which controls and knows everything
 * From this class the app get started and events get handled
 * @author carlr
 *
 */
public class AppController implements IPresenter, ValueChangeHandler<String> {

	private static final Logger LOGGER = Logger.getLogger(AppController.class.getName());

	private static final int MAX_FAVORITE_SENSORS = 1000;

	/**
	 * Handler manager mechanism for passing events and registering to be
	 * notified of some of these events.
	 */
	private final HandlerManager eventBus;

	/**
	 * GUI Image Bundle
	 */
	protected GUIImageBundle gui = GWT.create(GUIImageBundle.class);

	/**
	 *  Presenter
	 */
	private DataPanelPresenter dataPanelPresenter = null;
	private NavigationPanelPresenter navigationPanelPresenter = null;
	private FooterPresenter footerPresenter = null;

	/**
	 * Views
	 */
	private DataPanelViewImpl dataPanelView = null;
	private NavigationPanelViewImpl navigationPanelView = null;
	private FooterViewImpl footerView = null;


	private boolean isGuest = true;

	/**
	 * Constructs the application controller (main presenter).
	 *
	 * @param eventBus
	 */
	public AppController(final HandlerManager eventBus) {
		this.eventBus = eventBus;
		AuthenticationService.Util.getInstance().createUserInSession(new DefaultAsyncCallback<Boolean>(result -> {
			if((result != null) && result) {
				this.onUserLoggedIn(false);
			}
		}));
		this.bindHandler();
		this.setLanguageFromCookies();
		this.go(RootPanel.get());
		this.handleStart();
	}

	private void handleStart() {
		if((History.getToken() != null) && History.getToken().isEmpty()) {
			History.newItem(DataPanelPage.HOME.name(), true);
		}else {
			History.replaceItem(History.getToken(), true);
		}
	}

	private void bindHandler() {
		History.addValueChangeHandler(this);

		/**
		 * opens the page with the given parameters and fires the event
		 */
		this.eventBus.addHandler(OpenDataPanelPageEvent.TYPE, event -> {
			if(event.getParameters() != null) {
				History.newItem(event.getDataPanelPage().name() + this.getParamterAsString(event.getParameters()), event.isFireEvent());
			}else if(event.getIds() != null) {
				History.newItem(event.getDataPanelPage().name() + this.getIdAsString(event.getIds()), event.isFireEvent());
			}else {
				History.newItem(event.getDataPanelPage().name(), event.isFireEvent());
			}
		});

		this.eventBus.addHandler(AddSensorsToFavoriteListEvent.TYPE, event -> {
			List<Integer> favIds = CookieManager.getFavoriteList();
			List<Integer> notAdded = new ArrayList<>();
			if((favIds.size() + event.getIds().size()) > MAX_FAVORITE_SENSORS) {
				showError(Languages.maxFavoriteSensorsReached(MAX_FAVORITE_SENSORS));
				return;
			}
			StringBuilder string = new StringBuilder();
			event.getIds().stream().forEach(sensorId -> {
				if(!favIds.contains(sensorId)) {
					favIds.add(sensorId);
					string.append(sensorId + ", ");
				}else {
					notAdded.add(sensorId);
				}
			});
			if(!string.toString().substring(0, string.length() - 2).isEmpty()) {
				showSuccess(Languages.addedSensorsToList(string.toString().substring(0, string.length() - 2), Languages.favorites(), (event.getIds().size() - notAdded.size()) > 1));
			}
			if(!notAdded.isEmpty()) {
				showInfo(Languages.containsAlready(Arrays.toString(notAdded.toArray()).replace("[", "").replace("]", ""), Languages.favorites(), notAdded.size() > 1));
			}
			CookieManager.writeFavoriteListCookie(favIds);
			this.dataPanelPresenter.updateFavoriteList();
		});

		this.eventBus.addHandler(RemoveSensorsFromFavoriteListEvent.TYPE, event -> {
			List<Integer> favIds = CookieManager.getFavoriteList();
			StringBuilder string = new StringBuilder();
			event.getIds().stream().filter(favIds::contains).forEach(sensorId -> {
				favIds.remove(sensorId);
				string.append(sensorId + ", ");
			});
			showSuccess(Languages.removeSensorsFromList(string.toString().substring(0, string.length() - 2), Languages.favorites(), event.getIds().size() > 1));
			CookieManager.writeFavoriteListCookie(favIds);
			this.dataPanelPresenter.updateFavoriteList();
		});

		this.eventBus.addHandler(StartTourEvent.TYPE,  event -> {
			final List<String> finshedTours = CookieManager.getFinishedTours();
			if(event.getUserStartedTourEvent() || (CookieManager.getShowAutomaticallyTours() && !finshedTours.contains(event.getTour().name()))){
				if(!finshedTours.contains(event.getTour().name())) {
					finshedTours.add(event.getTour().name());
					CookieManager.saveFinishTours(finshedTours);
				}
				TourBuilder.getInstance(this.eventBus).startTour(event.getTour(), event.getUserStartedTourEvent());
			}
		});

		this.eventBus.addHandler(CloseTourEvent.TYPE,  event -> {
			if((event.getNeverShowToursAgain() != null) && event.getNeverShowToursAgain()) {
				CookieManager.setShowToursAutomatically(false);
			}
			TourBuilder.getInstance(this.eventBus).closeTour();
		});
	}


	@Override
	public void go(HasWidgets container) {
		HasWidgets controlPanel = RootPanel.get("control-panel");
		controlPanel.clear();
		if (this.navigationPanelView == null) {
			this.navigationPanelView = new NavigationPanelViewImpl();
		}
		this.navigationPanelPresenter = new NavigationPanelPresenter(this.eventBus, this, this.navigationPanelView);
		this.navigationPanelPresenter.go(controlPanel);

		HasWidgets dataPanelContainer = RootPanel.get("data-panel");
		dataPanelContainer.clear();
		if (this.dataPanelView == null) {
			this.dataPanelView = new DataPanelViewImpl();
		}
		this.dataPanelPresenter = new DataPanelPresenter(this.eventBus, this, this.dataPanelView);
		this.dataPanelPresenter.go(dataPanelContainer);

		HasWidgets footerContainer = RootPanel.get("footer");
		footerContainer.clear();
		if (this.footerView == null) {
			this.footerView = new FooterViewImpl();
		}
		this.footerPresenter = new FooterPresenter(this.eventBus, this, this.footerView);
		this.footerPresenter.go(footerContainer);
	}

	/**
	 * gets called if the history get changed and the event gets fired
	 * tries to parse the eventValue and open the given page with given parameters
	 * @logs invalid page or parameters in console and opens the homePage on default
	 */
	@Override
	public void onValueChange(ValueChangeEvent<String> event) {
		if (this.dataPanelPresenter == null) {
			LOGGER.log(Level.WARNING, "NAVIGATION: The dataPanelPresenter is null.");
			return;
		}
		if((event.getValue() == null) && !event.getValue().isEmpty()) {
			return;
		}
		String pageString = event.getValue();
		DataPanelPage page = null;
		Map<ParamType, String> parameters = null;
		List<Integer> ids = null;
		try {
			if(event.getValue().contains("?")) {
				page = DataPanelPage.valueOf(pageString.substring(0, pageString.indexOf('?')));
				parameters = this.getParameterAsMap(pageString.substring(pageString.indexOf('?'), pageString.length()));
			}else if(event.getValue().contains("/")) {
				page = DataPanelPage.valueOf(pageString.substring(0, pageString.indexOf('/')));
				ids = this.getIdsAsList(pageString.substring(pageString.indexOf('/'), pageString.length()));
			}else {
				page = DataPanelPage.valueOf(pageString);
			}
		}catch(Exception e) {
			page = DataPanelPage.HOME;
			LOGGER.log(Level.WARNING, "Invalid navigation page or invalid parameters.");
		}

		this.dataPanelPresenter.navigateTo(page, parameters, ids);
		this.navigationPanelPresenter.setActiveDataPanelPage(page);
	}

	/**
	 *
	 * @param parameterAsString
	 * @returns the parameterString as Map<ParamType, String>
	 */
	private Map<ParamType, String> getParameterAsMap(String parameterAsString){
		parameterAsString = parameterAsString.replace("?", "");
		final Map<ParamType, String> validParameters = new EnumMap<>(ParamType.class);
		String[] params;
		if(parameterAsString.contains("&")) {
			params = parameterAsString.split("&");
		}else {
			params = new String[]{parameterAsString};
		}
		for (String parameter : params) {
			if(!parameter.isEmpty() && parameter.contains("=")) {
				String[] keyValue = parameter.split("=");
				if((keyValue != null) && (keyValue[0] != null) && (keyValue[1] != null)) {
					boolean isValid = false;
					for (ParamType type : ParamType.values()) {
						if(type.getValue().equalsIgnoreCase(keyValue[0])) {
							isValid = true;
							validParameters.put(type, keyValue[1]);
							break;
						}
					}
					if(!isValid) {
						LOGGER.log(Level.WARNING, () -> "Parameter key is not valid: "+ keyValue[0]);
					}
				}
			}else {
				LOGGER.log(Level.WARNING, "Paramter is not valid, do not cotains \"=\"");
			}
		}
		return validParameters;
	}

	private List<Integer> getIdsAsList(String idsAsString){
		idsAsString = idsAsString.replace("/", "");
		final List<Integer> idsAsList = new ArrayList<>();
		String[] ids;
		if(idsAsString.contains(",")) {
			ids = idsAsString.split(",");
		}else {
			ids = new String[]{idsAsString};
		}
		for (String id : ids) {
			//only numbers will be accepted and parsed
			if(id.matches("^[0-9]*$")) {
				idsAsList.add(Integer.valueOf(id));
			}else{
				LOGGER.log(Level.WARNING, "The id contains not a number, will be removed");
			}
		}
		return idsAsList;
	}

	/**
	 * @param parameterList
	 * @returns the given parameterMap as String
	 */
	public String getParamterAsString(List<Parameter> parameterList) {
		StringBuilder parameters = new StringBuilder();
		if(!parameterList.isEmpty()) {
			parameters.append('?');
		}
		parameterList.forEach(param -> {
			parameters.append(param.getKey());
			parameters.append('=');
			parameters.append(param.getValue());
			parameters.append('&');
		});
		parameters.deleteCharAt(parameters.length() - 1);
		return parameters.toString();
	}

	private String getIdAsString(List<Integer> idsList) {
		StringBuilder idsAsString = new StringBuilder();
		if(!idsList.isEmpty()) {
			idsAsString.append('/');
		}
		idsList.forEach(id -> {
			idsAsString.append(id);
			idsAsString.append(',');
		});
		idsAsString.deleteCharAt(idsAsString.length() - 1);
		return idsAsString.toString();
	}

	public void switchLanguage(String lang) {
		if(lang.equals(Languages.getActualLanguageString())) {
			return;
		}
		if(Languages.GERMAN.equals(lang)) {
			Languages.setGerman();
		}else if(Languages.ENGLISH.equals(lang)) {
			Languages.setEnglish();
		}else {
			Languages.setSpanish();
		}
		CookieManager.writeLanguageCookie(Languages.getActualLanguageString());
		Window.Location.reload();
	}

	private void setLanguageFromCookies() {
		String lang = CookieManager.getLanguage();
		if ((lang != null)) {
			if(Languages.GERMAN.equals(lang)) {
				Languages.setGerman();
			}else if(Languages.ENGLISH.equals(lang)) {
				Languages.setEnglish();
			}else {
				Languages.setSpanish();
			}
		}else {
			lang = Languages.getActualLanguageString(); // Default is german
		}
		// Sets the correct serverLanguage.
		GeneralService.Util.getInstance().setServerLanguage(lang, new DefaultAsyncCallback<Void>(result -> {}));
	}

	public boolean isGuest() {
		return this.isGuest;
	}

	public void logout() {
		AuthenticationService.Util.getInstance().userLoggedOut(new DefaultAsyncCallback<ActionResult>(result -> {
			if((result != null) && ActionResultType.SUCCESSFUL.equals(result.getActionResultType())) {
				this.onUserLoggedOut();
			}
		}));
	}

	public void onUserLoggedIn(boolean goToHOme) {
		this.isGuest = false;
		if(goToHOme || (this.dataPanelPresenter.getActiveDataPanelPagePresenter() instanceof UserPresenter)) {
			this.eventBus.fireEvent(new OpenDataPanelPageEvent(DataPanelPage.HOME, true));
		}
		showSuccess(Languages.loggedIn());
		this.navigationPanelPresenter.setLastButtonActive(true);
		this.dataPanelPresenter.onUserLoggedIn();
	}

	public void onUserLoggedOut() {
		this.isGuest = true;
		showSuccess(Languages.loggedOut());
		if((this.dataPanelPresenter.getActiveDataPanelPagePresenter() instanceof UserPresenter)) {
			this.eventBus.fireEvent(new OpenDataPanelPageEvent(DataPanelPage.HOME, true));
		}
		this.navigationPanelPresenter.setLastButtonActive(false);
		this.dataPanelPresenter.onUserLoggedOut();
	}

	private static final int MAX_TOASTS = 4;
	private static final String TOAST_CONTAINER_ID = "toast-container";

	public static void showError(String message) {
		Element toastContainer = Document.get().getElementById(TOAST_CONTAINER_ID);
		int toastCount = (toastContainer!=null) ? toastContainer.getChildCount() : 0;
		if(toastCount<=MAX_TOASTS) {
			MaterialToast.fireToast(message, 4000, "error-growl");
		}
	}

	public static void showSuccess(String message) {
		Element toastContainer = Document.get().getElementById(TOAST_CONTAINER_ID);
		int toastCount = (toastContainer!=null) ? toastContainer.getChildCount() : 0;
		if(toastCount<=MAX_TOASTS) {
			MaterialToast.fireToast(message, 4000, "success-growl");
		}
	}

	public static void showInfo(String message) {
		Element toastContainer = Document.get().getElementById(TOAST_CONTAINER_ID);
		int toastCount = (toastContainer!=null) ? toastContainer.getChildCount() : 0;
		if(toastCount<=MAX_TOASTS) {
			MaterialToast.fireToast(message, 4000, "info-growl");
		}
	}

	public static void showLongInfo(String message) {
		Element toastContainer = Document.get().getElementById(TOAST_CONTAINER_ID);
		int toastCount = (toastContainer!=null) ? toastContainer.getChildCount() : 0;
		if(toastCount<=MAX_TOASTS) {
			MaterialToast.fireToast(message, 10000, "info-growl");
		}
	}

	public DataPanelPresenter getDataPanelPresenter() {
		return this.dataPanelPresenter;
	}
}
