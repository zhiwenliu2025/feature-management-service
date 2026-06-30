package com.fms.console.shared.ui;

import com.fms.console.admin.ui.ApplicationListView;
import com.fms.console.admin.ui.EnvironmentListView;
import com.fms.console.audit.ui.AuditLogView;
import com.fms.console.dashboard.ui.DashboardView;
import com.fms.console.explain.ui.ExplainView;
import com.fms.console.flag.ui.FlagListView;
import com.fms.console.release.ui.ReleaseListView;
import com.fms.console.shared.ui.components.UserMenu;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout implements RouterLayout {

  private final AccessControlService accessControl;
  private final Div breadcrumbSlot = new Div();
  private final Div bannerSlot = new Div();
  private final Div contentWrapper = new Div();
  private final VerticalLayout contentChrome = new VerticalLayout();

  public MainLayout(
      AccessControlService accessControl,
      GlobalContextBar contextBar,
      LayoutUiService layoutUiService) {
    this.accessControl = accessControl;
    layoutUiService.register(this::setBreadcrumb, this::showKillSwitchBanner, this::clearBanner);

    addClassName("fms-app-shell");
    setPrimarySection(Section.DRAWER);
    addToDrawer(createDrawer());
    addToNavbar(createNavbar(contextBar));

    bannerSlot.addClassName("fms-banner-slot");
    breadcrumbSlot.addClassName("fms-breadcrumb-slot");
    contentWrapper.addClassName("fms-content-wrapper");
    contentWrapper.setSizeFull();

    contentChrome.addClassName("fms-content-chrome");
    contentChrome.setPadding(false);
    contentChrome.setSpacing(false);
    contentChrome.setSizeFull();
    contentChrome.add(bannerSlot, breadcrumbSlot, contentWrapper);
    contentChrome.expand(contentWrapper);
    setContent(contentChrome);
  }

  @Override
  public void showRouterLayoutContent(HasElement content) {
    contentWrapper.removeAll();
    if (content != null) {
      contentWrapper.getElement().appendChild(content.getElement());
    }
  }

  public void setBreadcrumb(Component breadcrumb) {
    breadcrumbSlot.removeAll();
    if (breadcrumb != null) {
      breadcrumbSlot.add(breadcrumb);
    }
  }

  public void showKillSwitchBanner(String message) {
    bannerSlot.removeAll();
    Div banner = new Div();
    banner.setText(message);
    banner.addClassName("fms-kill-switch-banner");
    bannerSlot.add(banner);
  }

  public void clearBanner() {
    bannerSlot.removeAll();
  }

  private VerticalLayout createDrawer() {
    VerticalLayout drawer = new VerticalLayout();
    drawer.addClassName("fms-drawer");
    drawer.setPadding(false);
    drawer.setSpacing(false);
    drawer.setSizeFull();

    Div header = new Div();
    header.addClassName("fms-drawer-header");

    Icon brandIcon = VaadinIcon.FLAG.create();
    brandIcon.addClassName("fms-drawer-brand-icon");

    H1 brandTitle = new H1("FMS");
    brandTitle.addClassName("fms-drawer-brand-title");

    Paragraph brandSubtitle = new Paragraph("Feature Management");
    brandSubtitle.addClassName("fms-drawer-brand-subtitle");

    Div brandText = new Div(brandTitle, brandSubtitle);
    brandText.addClassName("fms-drawer-brand-text");

    Div brand = new Div(brandIcon, brandText);
    brand.addClassName("fms-drawer-brand");
    header.add(brand);

    SideNav nav = createNavigation();
    nav.addClassName("fms-drawer-nav");

    drawer.add(header, nav);
    drawer.expand(nav);
    return drawer;
  }

  private SideNav createNavigation() {
    SideNav nav = new SideNav();

    if (accessControl.canReadFlags()) {
      nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
      nav.addItem(new SideNavItem("Feature Flags", FlagListView.class, VaadinIcon.FLAG.create()));
      nav.addItem(new SideNavItem("Releases", ReleaseListView.class, VaadinIcon.PACKAGE.create()));
    }
    if (accessControl.canExplain()) {
      nav.addItem(new SideNavItem("Explain", ExplainView.class, VaadinIcon.SEARCH.create()));
    }
    if (accessControl.canReadAudit()) {
      nav.addItem(new SideNavItem("Audit Log", AuditLogView.class, VaadinIcon.LIST.create()));
    }
    if (accessControl.isAdmin()) {
      nav.addItem(new SideNavItem("Applications", ApplicationListView.class, VaadinIcon.GRID_BIG.create()));
      nav.addItem(new SideNavItem("Environments", EnvironmentListView.class, VaadinIcon.CLUSTER.create()));
    }

    return nav;
  }

  private HorizontalLayout createNavbar(GlobalContextBar contextBar) {
    HorizontalLayout navbar = new HorizontalLayout();
    navbar.addClassName("fms-navbar");
    navbar.setWidthFull();
    navbar.setAlignItems(FlexComponent.Alignment.CENTER);
    navbar.setPadding(true);
    navbar.setSpacing(true);

    DrawerToggle toggle = new DrawerToggle();

    Span navbarBrand = new Span("FMS Admin");
    navbarBrand.addClassName("fms-navbar-brand");
    HorizontalLayout brandArea = new HorizontalLayout(toggle, navbarBrand);
    brandArea.setAlignItems(FlexComponent.Alignment.CENTER);
    brandArea.setSpacing(true);

    Div contextArea = new Div(contextBar);
    contextArea.addClassName("fms-navbar-context");

    Div actionsArea = new Div(new UserMenu());
    actionsArea.addClassName("fms-navbar-actions");

    navbar.add(brandArea, contextArea, actionsArea);
    navbar.expand(contextArea);
    return navbar;
  }
}
