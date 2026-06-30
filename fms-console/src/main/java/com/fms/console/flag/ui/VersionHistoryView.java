package com.fms.console.flag.ui;

import com.fms.common.api.PageResponse;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.FlagDtos.FlagDetailDto;
import com.fms.console.client.dto.FlagDtos.FlagVersionDetailDto;
import com.fms.console.client.dto.FlagDtos.FlagVersionSummaryDto;
import com.fms.console.client.dto.FlagDtos.RollbackFlagDto;
import com.fms.console.flag.service.FlagUiService;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.GlobalContextBar;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.RouteLinks;
import com.fms.console.shared.ui.UiFormat;
import com.fms.console.shared.ui.components.DiffPanel;
import com.fms.console.shared.ui.components.EmptyState;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.FmsConfirmDialog;
import com.fms.console.shared.ui.components.FmsNotification;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route(value = "flags/:flagKey/versions", layout = MainLayout.class)
@PermitAll
public class VersionHistoryView extends VerticalLayout implements BeforeEnterObserver {

  private final FlagUiService flagUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;

  private String flagKey;
  private final Grid<FlagVersionSummaryDto> grid = new Grid<>(FlagVersionSummaryDto.class, false);
  private final VerticalLayout gridContainer = new VerticalLayout();
  private final VerticalLayout diffArea = new VerticalLayout();

  public VersionHistoryView(FlagUiService flagUiService, AccessControlService accessControl, LayoutUiService layoutUi) {
    this.flagUiService = flagUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;
    setPadding(true);
    setSpacing(true);
    setSizeFull();

    H2 title = new H2("Version history");
    title.addClassName("fms-page-title");
    configureGrid();
    gridContainer.setPadding(false);
    gridContainer.setSpacing(true);
    gridContainer.setSizeFull();
    gridContainer.add(grid);
    add(title, gridContainer, diffArea);
    setFlexGrow(1, gridContainer);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.canReadFlags()) {
      event.rerouteTo(ForbiddenView.class);
      return;
    }
    flagKey = event.getRouteParameters().get("flagKey").orElse("");
    layoutUi.setBreadcrumb(new FmsBreadcrumb()
        .segment("Flags", FlagListView.class)
        .segment(flagKey, FlagDetailView.class, RouteLinks.flagParams(flagKey))
        .current("Versions"));
    load();
  }

  private void configureGrid() {
    grid.addColumn(FlagVersionSummaryDto::flagVersion).setHeader("Version");
    grid.addColumn(FlagVersionSummaryDto::configVersion).setHeader("Config version");
    grid.addColumn(v -> UiFormat.formatInstant(v.publishedAt())).setHeader("Published at");
    grid.addColumn(FlagVersionSummaryDto::publishedBy).setHeader("Published by");
    grid.addColumn(FlagVersionSummaryDto::comment).setHeader("Comment");
    grid.addComponentColumn(v -> {
      Button view = new Button("Diff", e -> showDiff(v.flagVersion()));
      Button rollback = new Button("Rollback", e -> rollback(v.flagVersion()));
      rollback.setEnabled(accessControl.canPublish());
      return new com.vaadin.flow.component.orderedlayout.HorizontalLayout(view, rollback);
    }).setHeader("Actions");
    grid.setSizeFull();
  }

  private void updateEmptyState(List<FlagVersionSummaryDto> items) {
    gridContainer.removeAll();
    if (items.isEmpty()) {
      gridContainer.add(new EmptyState(
          "No published versions",
          "Publish this flag to create version history."));
    } else {
      gridContainer.add(grid);
    }
  }

  private void load() {
    try {
      PageResponse<FlagVersionSummaryDto> page = flagUiService.listVersions(
          GlobalContextBar.resolveAppId(), flagKey, GlobalContextBar.resolveEnvironment(), 50);
      grid.setItems(page.data());
      updateEmptyState(page.data());
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private void showDiff(int version) {
    try {
      FlagVersionDetailDto snapshot = flagUiService.getVersion(
          GlobalContextBar.resolveAppId(), flagKey, version, GlobalContextBar.resolveEnvironment());
      FlagDetailDto current = flagUiService.getFlag(GlobalContextBar.resolveAppId(), flagKey);
      diffArea.removeAll();
      diffArea.add(new DiffPanel(current, snapshot.snapshot()));
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private void rollback(int version) {
    FmsConfirmDialog.confirmDestructive(
        "Rollback",
        "Rollback " + flagKey + " to version " + version + "?",
        () -> {
          try {
            flagUiService.rollback(flagKey, new RollbackFlagDto(
                GlobalContextBar.resolveAppId(),
                GlobalContextBar.resolveEnvironment(),
                version,
                "Rollback from console"));
            FmsNotification.success("Rollback started.");
            load();
          } catch (FmsUiException ex) {
            ApiClientExceptionHandler.handle(ex);
          }
        });
  }
}
