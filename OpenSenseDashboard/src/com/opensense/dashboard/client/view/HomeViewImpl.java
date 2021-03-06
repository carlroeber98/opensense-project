package com.opensense.dashboard.client.view;

import java.util.ArrayList;

import org.gwtbootstrap3.client.ui.Heading;
import org.gwtbootstrap3.client.ui.html.Div;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Widget;
import com.opensense.dashboard.client.event.StartTourEvent;
import com.opensense.dashboard.client.utils.CookieManager;
import com.opensense.dashboard.client.utils.Languages;
import com.opensense.dashboard.client.utils.tourutils.Tours;

import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialCheckBox;

public class HomeViewImpl extends DataPanelPageView implements HomeView {

	@UiTemplate("HomeView.ui.xml")
	interface HomeViewUiBinder extends UiBinder<Widget, HomeViewImpl> {
	}

	private static HomeViewUiBinder uiBinder = GWT.create(HomeViewUiBinder.class);

	protected Presenter presenter;

	/*welcomePageCard*/
	@UiField
	Heading userInfo;
	@UiField
	Span welcomeText;
	@UiField
	Span welcomeText1;
	@UiField
	Span welcomeText2;

	/* searchPage */
	@UiField
	Div searchCard;
	@UiField
	Heading searchCardInfo;
	@UiField
	Span searchCardText;

	/* mapPage */
	@UiField
	Div mapCard;
	@UiField
	Heading mapCardInfo;
	@UiField
	Span mapCardText;

	/* visuPage */
	@UiField
	Div visuCard;
	@UiField
	Heading visuCardInfo;
	@UiField
	Span visuCardText;

	/* listPage */
	@UiField
	Div listCard;
	@UiField
	Heading listCardInfo;
	@UiField
	Span listCardText;

	@UiField
	MaterialCheckBox alwaysShowCookiesCheckBox;
	@UiField
	MaterialButton resetCookiesButton;
	
	private static final String WHITE_COLOR = "white";

	public HomeViewImpl() {
		this.initWidget(uiBinder.createAndBindUi(this));
		this.triggerEventOnCardClick();
		this.setAllCardInfos();
	}

	@UiHandler("alwaysShowCookiesCheckBox")
	public void onAlwaysShowCookiesCheckBoxButtonClicked(ValueChangeEvent<Boolean> e) {
		CookieManager.setShowToursAutomatically(e.getValue());
	}

	@UiHandler("resetCookiesButton")
	public void onResetCookiesButtonClicked(ClickEvent e) {
		CookieManager.saveFinishTours(new ArrayList<>());
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void initView() {
		// init UI Elements if needed
	}

	@Override
	public void setUserInfo(String userInfo) {
		this.userInfo.setText(userInfo);
	}

	public void setAllCardInfos() {
		this.setWelcomeCardText();
		this.setSearchCardText();
		this.setMapCardText();
		this.setVisuCardText();
		this.setListCardText();
	}

	public void setWelcomeCardText(){
		Span[] welcomeTexts = {welcomeText,welcomeText1,welcomeText2};
		String[] welcomeInfoTexts = {Languages.welcomeInfoText(),Languages.welcomeInfoText1(),Languages.welcomeInfoText2()};
		for (int i = 0; i < welcomeTexts.length; i++) {
			welcomeTexts[i].setText(welcomeInfoTexts[i]);
			welcomeTexts[i].setColor(WHITE_COLOR);
		}
	}


	public void setSearchCardText(){
		this.searchCardInfo.setText(Languages.search());
		this.searchCardText.setText(Languages.searchInfoText());
		this.searchCardInfo.setColor(WHITE_COLOR);
	}


	public void setMapCardText(){
		this.mapCardInfo.setText(Languages.map());
		this.mapCardText.setText(Languages.mapInfoText());
		this.mapCardInfo.setColor(WHITE_COLOR);
	}



	public void setVisuCardText(){
		this.visuCardInfo.setText(Languages.graphics());
		this.visuCardText.setText(Languages.visuInfoText());
		this.visuCardInfo.setColor(WHITE_COLOR);
	}

	public void setListCardText(){
		this.listCardInfo.setText(Languages.list());
		this.listCardText.setText(Languages.listInfoText());
		this.listCardInfo.setColor(WHITE_COLOR);
	}

	public void triggerEventOnCardClick() {
		this.searchCard.addDomHandler(event -> this.presenter.getEventBus().fireEvent(new StartTourEvent(Tours.SEARCH_PAGE, true)), ClickEvent.getType());
		this.mapCard.addDomHandler(event -> this.presenter.getEventBus().fireEvent(new StartTourEvent(Tours.MAP_PAGE, true)), ClickEvent.getType());
		this.listCard.addDomHandler(event -> this.presenter.getEventBus().fireEvent(new StartTourEvent(Tours.LIST_PAGE, true)), ClickEvent.getType());
		this.visuCard.addDomHandler(event -> this.presenter.getEventBus().fireEvent(new StartTourEvent(Tours.VIS_PAGE, true)), ClickEvent.getType());
	}

	public void setCheckboxValue() {
		this.alwaysShowCookiesCheckBox.setValue(CookieManager.getShowAutomaticallyTours());
	}

}
