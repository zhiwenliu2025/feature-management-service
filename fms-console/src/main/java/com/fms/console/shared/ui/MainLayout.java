package com.fms.console.shared.ui;

import com.fms.console.admin.ui.ApplicationListView;
import com.fms.console.admin.ui.EnvironmentListView;
import com.fms.console.audit.ui.AuditLogView;
import com.fms.console.dashboard.ui.DashboardView;
import com.fms.console.explain.ui.ExplainView;
import com.fms.console.flag.ui.FlagListView;
import com.fms.console.release.ui.ReleaseListView;
import com.fms.console.shared.ui.components.UserMenu;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

  private final AccessControlService accessControl;
  private final Div breadcrumbSlot = new Div();
  private final Div bannerSlot = new Div();

  public MainLayout(
      AccessControlService accessControl,
      GlobalContextBar contextBar,
      LayoutUiService layoutUiService) {
    this.accessControl = accessControl;
    layoutUiService.register(this::setBreadcrumb, this::showKillSwitchBanner, this::clearBanner);
    setPrimarySection(Section.DRAWER);
    addToDrawer(createHeader(), createNavigation());
    addToNavbar(createNavbar(contextBar));

    breadcrumbSlot.addClassName("fms-breadcrumb-slot");
    bannerSlot.addClassName("fms-banner-slot");
    VerticalLayout wrapper = new VerticalLayout(bannerSlot, breadcrumbSlot);
    wrapper.setPadding(false);
    wrapper.setSpacing(false);
    wrapper.setWidthFull();
    addToNavbar(wrapper);
  }

  public void setBreadcrumb(com.vaadin.flow.component.Component breadcrumb) {
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

  private HorizontalLayout createHeader() {
    H1 title = new H1("FMS");
    title.getStyle().set("font-size", "var(--aura-font-size-l)");
    title.getStyle().set("margin", "0");

    HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title);
    header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    header.setWidthFull();
    header.addClassName("fms-drawer-header");
    return header;
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
    UserMenu userMenu = new UserMenu();

    HorizontalLayout navbar = new HorizontalLayout(contextBar, userMenu);
    navbar.setWidthFull();
    navbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    navbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    navbar.setPadding(true);
    navbar.setSpacing(true);
    return navbar;
  }
}
