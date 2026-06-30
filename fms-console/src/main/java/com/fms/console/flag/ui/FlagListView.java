package com.fms.console.flag.ui;

import jakarta.annotation.security.PermitAll;

import com.fms.common.api.PageResponse;
import com.fms.console.admin.service.EnvironmentUiService;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.FlagDtos.CreateFlagDto;
import com.fms.console.client.dto.FlagDtos.FlagSummaryDto;
import com.fms.console.flag.service.FlagUiService;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.GlobalContextBar;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.components.EmptyState;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.FmsConfirmDialog;
import com.fms.console.shared.ui.components.FmsNotification;
import com.fms.console.shared.ui.components.PageHeader;
import com.fms.console.shared.ui.components.PromoteDialog;
import com.fms.console.shared.ui.components.StatusBadge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.fms.console.shared.ui.RouteLinks;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "flags", layout = MainLayout.class)
@PermitAll
public class FlagListView extends VerticalLayout implements BeforeEnterObserver {

  private final FlagUiService flagUiService;
  private final EnvironmentUiService environmentUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;

  private final Grid<FlagSummaryDto> grid = new Grid<>(FlagSummaryDto.class, false);
  private final VerticalLayout gridContainer = new VerticalLayout();
  private final TextField search = new TextField();
  private final ComboBox<String> statusFilter = new ComboBox<>("Status");
  private String cursor;

  public FlagListView(
      FlagUiService flagUiService,
      EnvironmentUiService environmentUiService,
      AccessControlService accessControl,
      LayoutUiService layoutUi) {
    this.flagUiService = flagUiService;
    this.environmentUiService = environmentUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;

    setPadding(true);
    setSpacing(true);
    setSizeFull();

    search.setPlaceholder("Search flags…");
    search.setWidth("280px");
    statusFilter.setItems("draft", "published", "archived");
    statusFilter.setClearButtonVisible(true);

    Button refresh = new Button("Search", VaadinIcon.SEARCH.create(), e -> load(null));
    Button create = new Button("New flag", VaadinIcon.PLUS.create(), e -> openCreateDialog());
    create.setEnabled(accessControl.canWriteFlags());

    Button promote = new Button("Promote selected", VaadinIcon.UPLOAD.create(), e -> openPromote());
    promote.setEnabled(accessControl.canPublish());

    HorizontalLayout toolbar = new HorizontalLayout(search, statusFilter, refresh, create, promote);
    toolbar.setAlignItems(Alignment.END);
    toolbar.setWidthFull();

    configureGrid();
    gridContainer.setPadding(false);
    gridContainer.setSpacing(true);
    gridContainer.setSizeFull();
    gridContainer.add(grid);
    setFlexGrow(1, gridContainer);

    Button loadMore = new Button("Load more", e -> load(cursor));
    loadMore.setId("load-more");

    add(new PageHeader("Feature Flags"), toolbar, gridContainer, loadMore);

    load(null);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.canReadFlags()) {
      event.rerouteTo(ForbiddenView.class);
    }
    layoutUi.setBreadcrumb(new FmsBreadcrumb().current("Feature Flags"));
    layoutUi.clearBanner();
  }

  private void configureGrid() {
    grid.addClassName("fms-grid-compact");
    grid.setSelectionMode(Grid.SelectionMode.MULTI);
    grid.addColumn(FlagSummaryDto::key).setHeader("Key").setFlexGrow(1)
        .setRenderer(new ComponentRenderer<>(flag -> {
          var link = RouteLinks.flag(flag.key(), flag.key());
          link.addClassName("fms-monospace");
          return link;
        }));
    grid.addColumn(FlagSummaryDto::name).setHeader("Name").setFlexGrow(2);
    grid.addColumn(FlagSummaryDto::type).setHeader("Type");
    grid.addColumn(flag -> flag.status()).setHeader("Status")
        .setRenderer(new ComponentRenderer<>(f -> StatusBadge.forFlagStatus(f.status(), f.draftDirty())));
    grid.addColumn(f -> String.valueOf(f.defaultValue())).setHeader("Default");
    grid.addColumn(f -> f.tags() == null ? "" : String.join(", ", f.tags())).setHeader("Tags");
    grid.addColumn(f -> f.updatedAt() == null ? "" :
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(f.updatedAt()))
        .setHeader("Updated");

    grid.addComponentColumn(flag -> {
      Button menu = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
      menu.getElement().setAttribute("aria-label", "Actions for " + flag.key());
      ContextMenu cm = new ContextMenu(menu);
      cm.setOpenOnClick(true);
      if (accessControl.canWriteFlags()) {
        cm.addItem("Edit", e -> getUI().ifPresent(ui -> ui.navigate(FlagDetailView.class, RouteLinks.flagParams(flag.key()))));
      }
      if (accessControl.canPublish()) {
        cm.addItem("Publish", e -> getUI().ifPresent(ui -> ui.navigate(FlagDetailView.class, RouteLinks.flagParams(flag.key()))));
      }
      if (accessControl.canKillSwitch()) {
        cm.addItem("Kill switch", e -> getUI().ifPresent(ui -> ui.navigate(FlagDetailView.class, RouteLinks.flagParams(flag.key()))));
      }
      if (accessControl.canWriteFlags()) {
        cm.addItem("Archive", e -> FmsConfirmDialog.confirmDestructive(
            "Archive flag",
            "Archive " + flag.key() + "?",
            () -> archive(flag.key())));
      }
      return menu;
    }).setHeader("Actions").setFlexGrow(0).setWidth("80px");
    grid.setSizeFull();
  }

  private void updateEmptyState(List<FlagSummaryDto> items) {
    gridContainer.removeAll();
    if (items.isEmpty()) {
      String query = search.getValue();
      if (query != null && !query.isBlank()) {
        gridContainer.add(new EmptyState(
            "No flags found",
            "No flags match your search. Try a different term or clear filters.",
            "Clear search",
            () -> {
              search.clear();
              load(null);
            }));
      } else {
        gridContainer.add(new EmptyState(
            "No feature flags",
            "Create your first flag to get started.",
            accessControl.canWriteFlags() ? "New flag" : null,
            accessControl.canWriteFlags() ? this::openCreateDialog : null));
      }
    } else {
      gridContainer.add(grid);
    }
  }

  private void load(String nextCursor) {
    try {
      PageResponse<FlagSummaryDto> page = flagUiService.listFlags(
          GlobalContextBar.resolveAppId(),
          statusFilter.getValue(),
          null,
          search.getValue(),
          20,
          nextCursor);
      List<FlagSummaryDto> items;
      if (nextCursor == null) {
        items = page.data();
      } else {
        items = new ArrayList<>(grid.getListDataView().getItems().toList());
        items.addAll(page.data());
      }
      grid.setItems(items);
      updateEmptyState(items);
      cursor = page.pagination().nextCursor();
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private void openCreateDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Create feature flag");
    TextField key = new TextField("Key");
    TextField name = new TextField("Name");
    ComboBox<String> type = new ComboBox<>("Type");
    type.setItems("boolean", "string", "number", "json");
    type.setValue("boolean");
    TextField defaultValue = new TextField("Default value");
    defaultValue.setValue("false");
    Button save = new Button("Create", e -> {
      try {
        Object def = "boolean".equals(type.getValue()) ? Boolean.parseBoolean(defaultValue.getValue()) : defaultValue.getValue();
        flagUiService.createFlag(new CreateFlagDto(
            GlobalContextBar.resolveAppId(),
            key.getValue(),
            name.getValue(),
            "",
            type.getValue(),
            def,
            List.of()));
        dialog.close();
        FmsNotification.success("Flag created.");
        load(null);
      } catch (FmsUiException ex) {
        ApiClientExceptionHandler.handle(ex);
      }
    });
    dialog.add(key, name, type, defaultValue, save);
    dialog.open();
  }

  private void archive(String flagKey) {
    try {
      flagUiService.archiveFlag(GlobalContextBar.resolveAppId(), flagKey);
      FmsNotification.success("Flag archived.");
      load(null);
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private void openPromote() {
    var selected = grid.getSelectedItems().stream().map(FlagSummaryDto::key).toList();
    if (selected.isEmpty()) {
      FmsNotification.error("Select at least one flag.");
      return;
    }
    new PromoteDialog(environmentUiService, GlobalContextBar.resolveAppId(), selected, () -> load(null)).open();
  }
}
