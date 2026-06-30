package com.fms.console.flag.ui;

import jakarta.annotation.security.PermitAll;

import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.FlagDtos.FlagDetailDto;
import com.fms.console.client.dto.FlagDtos.PublishFlagDto;
import com.fms.console.client.dto.FlagDtos.UpdateFlagDto;
import com.fms.console.client.dto.KillSwitchDtos.KillSwitchDto;
import com.fms.console.client.dto.KillSwitchDtos.KillSwitchListDto;
import com.fms.console.client.dto.KillSwitchDtos.KillSwitchRequestDto;
import com.fms.console.flag.service.FlagUiService;
import com.fms.console.flag.service.KillSwitchUiService;
import com.fms.console.release.service.ReleaseUiService;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.GlobalContextBar;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.FmsConfirmDialog;
import com.fms.console.shared.ui.components.FmsNotification;
import com.fms.console.shared.ui.components.PageHeader;
import com.fms.console.shared.ui.components.PublishJobTracker;
import com.fms.console.shared.ui.components.StatusBadge;
import com.fms.console.shared.ui.components.UnsavedChangesGuard;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.fms.console.shared.ui.RouteLinks;
import com.vaadin.flow.router.RouteParameters;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Route(value = "flags/:flagKey", layout = MainLayout.class)
@PermitAll
public class FlagDetailView extends VerticalLayout implements BeforeEnterObserver {

  private final FlagUiService flagUiService;
  private final KillSwitchUiService killSwitchUiService;
  private final ReleaseUiService releaseUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;

  private String flagKey;
  private FlagDetailDto flag;

  private final VerticalLayout content = new VerticalLayout();
  private final Div draftBar = new Div();
  private final PublishJobTracker publishTracker;

  public FlagDetailView(
      FlagUiService flagUiService,
      KillSwitchUiService killSwitchUiService,
      ReleaseUiService releaseUiService,
      AccessControlService accessControl,
      LayoutUiService layoutUi) {
    this.flagUiService = flagUiService;
    this.killSwitchUiService = killSwitchUiService;
    this.releaseUiService = releaseUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;
    this.publishTracker = new PublishJobTracker(flagUiService);

    setPadding(true);
    setSpacing(true);
    setSizeFull();
    draftBar.addClassName("fms-draft-bar");
    add(content, draftBar, publishTracker);
    setFlexGrow(1, content);
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
        .current(flagKey));
    refresh();
  }

  private void refresh() {
    try {
      flag = flagUiService.getFlag(GlobalContextBar.resolveAppId(), flagKey);
      render();
      checkKillSwitchBanner();
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private void checkKillSwitchBanner() {
    try {
      KillSwitchListDto list = killSwitchUiService.listActive(
          GlobalContextBar.resolveAppId(), flagKey, GlobalContextBar.resolveEnvironment());
      if (list.overrides() != null && list.overrides().stream().anyMatch(KillSwitchDto::isActive)) {
        layoutUi.showKillSwitchBanner("Kill switch is active for " + flagKey);
      } else {
        layoutUi.clearBanner();
      }
    } catch (Exception ex) {
      layoutUi.clearBanner();
    }
  }

  private void render() {
    content.removeAll();
    String env = GlobalContextBar.resolveEnvironment();
    boolean draftDirty = flag.environmentStates().stream()
        .filter(s -> env.equals(s.environment()))
        .anyMatch(s -> s.draftDirty());

    String subtitle = flag.name() != null && !flag.name().isBlank()
        ? flag.name() + " · " + flag.type() + " · default: " + flag.defaultValue()
        : flag.type() + " · default: " + flag.defaultValue();

    PageHeader pageHeader = new PageHeader(
        flag.key(),
        subtitle,
        new com.vaadin.flow.component.Component[] {StatusBadge.forFlagStatus(flag.status(), draftDirty)},
        new com.vaadin.flow.component.Component[0]);

    Paragraph meta = new Paragraph(flag.appId() + " · environment: " + env);
    meta.addClassName("fms-placeholder");

    HorizontalLayout nav = new HorizontalLayout(
        RouteLinks.to("Rules", RuleEditorView.class, RouteLinks.flagParams(flagKey)),
        RouteLinks.to("Version history", VersionHistoryView.class, RouteLinks.flagParams(flagKey)));
    if (accessControl.canPublish()) {
      nav.add(new Button("Publish tab", e -> showPublishTab()));
    }

    Tab overview = new Tab("Overview");
    Tab publish = new Tab("Publish");
    Tab kill = new Tab("Kill Switch");
    Tabs tabs = new Tabs(overview, publish, kill);
    Div tabContent = new Div();
    tabContent.setWidthFull();
    tabs.addSelectedChangeListener(e -> {
      tabContent.removeAll();
      if (e.getSelectedTab() == overview) {
        tabContent.add(buildOverview());
      } else if (e.getSelectedTab() == publish) {
        tabContent.add(buildPublish());
      } else {
        tabContent.add(buildKillSwitch());
      }
    });
    tabs.setSelectedTab(overview);
    tabContent.add(buildOverview());

    content.add(pageHeader, meta, nav, tabs, tabContent);
    updateDraftBar(draftDirty);
  }

  private void showPublishTab() {
    content.getChildren()
        .filter(c -> c instanceof Tabs)
        .findFirst()
        .ifPresent(t -> ((Tabs) t).setSelectedIndex(1));
  }

  private VerticalLayout buildOverview() {
    FormLayout form = new FormLayout();
    TextField name = new TextField("Name");
    name.setValue(flag.name());
    TextArea description = new TextArea("Description");
    description.setValue(flag.description() == null ? "" : flag.description());
    TextField defaultValue = new TextField("Default value");
    defaultValue.setValue(String.valueOf(flag.defaultValue()));
    TextField tags = new TextField("Tags");
    tags.setValue(flag.tags() == null ? "" : String.join(",", flag.tags()));
    TextField keyField = new TextField("Key");
    keyField.setValue(flag.key());
    keyField.setReadOnly(true);
    form.add(keyField, name, description, defaultValue, tags);

    Button save = new Button("Save draft", VaadinIcon.CHECK.create(), e -> {
      try {
        Object def = flag.defaultValue() instanceof Boolean
            ? Boolean.parseBoolean(defaultValue.getValue())
            : defaultValue.getValue();
        List<String> tagList = tags.getValue().isBlank()
            ? List.of()
            : Arrays.stream(tags.getValue().split(",")).map(String::trim).toList();
        flagUiService.updateFlag(GlobalContextBar.resolveAppId(), flagKey,
            new UpdateFlagDto(name.getValue(), description.getValue(), def, tagList));
        UnsavedChangesGuard.markDirty(false);
        FmsNotification.success("Draft saved.");
        refresh();
      } catch (FmsUiException ex) {
        ApiClientExceptionHandler.handle(ex);
      }
    });
    save.setEnabled(accessControl.canWriteFlags());

    Button archive = new Button("Archive", VaadinIcon.ARCHIVE.create(), e -> FmsConfirmDialog.confirmDestructive(
        "Archive flag", "Archive " + flagKey + "?", () -> {
          try {
            flagUiService.archiveFlag(GlobalContextBar.resolveAppId(), flagKey);
            FmsNotification.success("Archived.");
            getUI().ifPresent(ui -> ui.navigate(FlagListView.class));
          } catch (FmsUiException ex) {
            ApiClientExceptionHandler.handle(ex);
          }
        }));
    archive.setEnabled(accessControl.canWriteFlags());

    VerticalLayout layout = new VerticalLayout(form, new HorizontalLayout(save, archive));
    layout.setPadding(false);
    return layout;
  }

  private VerticalLayout buildPublish() {
    VerticalLayout layout = new VerticalLayout();
    if (!accessControl.canPublish()) {
      layout.add(new Paragraph("You do not have publish permission."));
      return layout;
    }
    ComboBox<String> environment = new ComboBox<>("Environment");
    environment.setItems("dev", "staging", "prod");
    environment.setValue(GlobalContextBar.resolveEnvironment());
    ComboBox<String> release = new ComboBox<>("Release");
    try {
      release.setItems(releaseUiService.list(50).data().stream().map(r -> r.releaseId()).toList());
    } catch (Exception ignored) {
    }
    TextField comment = new TextField("Comment");
    comment.setRequiredIndicatorVisible(true);
    comment.setWidthFull();

    Button publish = new Button("Publish", VaadinIcon.UPLOAD.create(), e -> FmsConfirmDialog.confirm(
        "Publish flag",
        "Publish " + flagKey + " to " + environment.getValue() + "?",
        () -> {
          if (comment.isEmpty()) {
            FmsNotification.error("Comment is required.");
            return;
          }
          try {
            flagUiService.publish(GlobalContextBar.resolveAppId(), flagKey, new PublishFlagDto(
                environment.getValue(), release.getValue(), comment.getValue()));
            publishTracker.track(GlobalContextBar.resolveAppId(), flagKey, environment.getValue(), this::refresh);
            FmsNotification.success("Publish started.");
          } catch (FmsUiException ex) {
            ApiClientExceptionHandler.handle(ex);
          }
        }));
    layout.add(environment, release, comment, publish);
    return layout;
  }

  private VerticalLayout buildKillSwitch() {
    VerticalLayout layout = new VerticalLayout();
    if (!accessControl.canKillSwitch()) {
      layout.add(new Paragraph("You do not have kill switch permission."));
      return layout;
    }
    ComboBox<String> scope = new ComboBox<>("Scope");
    scope.setItems("global", "region");
    scope.setValue("global");
    TextField region = new TextField("Region code");
    TextField reason = new TextField("Reason / incident");
    reason.setWidthFull();
    ComboBox<String> forced = new ComboBox<>("Forced value");
    forced.setItems("false", "true");
    forced.setValue("false");

    Button activate = new Button("Activate kill switch", VaadinIcon.BAN.create(), e -> FmsConfirmDialog.confirmDestructive(
        "Activate kill switch",
        "This will force the flag off for the selected scope. Continue?",
        () -> {
          if (reason.isEmpty()) {
            FmsNotification.error("Reason is required.");
            return;
          }
          try {
            killSwitchUiService.activate(flagKey, new KillSwitchRequestDto(
                GlobalContextBar.resolveAppId(),
                GlobalContextBar.resolveEnvironment(),
                scope.getValue(),
                region.getValue(),
                Boolean.parseBoolean(forced.getValue()),
                reason.getValue()));
            FmsNotification.success("Kill switch activated.");
            refresh();
          } catch (FmsUiException ex) {
            ApiClientExceptionHandler.handle(ex);
          }
        }));
    activate.getElement().setAttribute("aria-label", "Activate kill switch");

    Button deactivate = new Button("Deactivate kill switch", VaadinIcon.CHECK.create(), e -> {
      try {
        killSwitchUiService.deactivate(flagKey, new KillSwitchRequestDto(
            GlobalContextBar.resolveAppId(),
            GlobalContextBar.resolveEnvironment(),
            scope.getValue(),
            region.getValue(),
            Boolean.parseBoolean(forced.getValue()),
            reason.getValue()));
        FmsNotification.success("Kill switch deactivated.");
        refresh();
      } catch (FmsUiException ex) {
        ApiClientExceptionHandler.handle(ex);
      }
    });

    layout.add(scope, region, forced, reason, new HorizontalLayout(activate, deactivate));
    return layout;
  }

  private void updateDraftBar(boolean dirty) {
    draftBar.removeAll();
    UnsavedChangesGuard.markDirty(dirty);
    if (!dirty) {
      draftBar.setVisible(false);
      return;
    }
    draftBar.setVisible(true);
    draftBar.add(new Span("Draft modified, not yet published."));
    if (accessControl.canPublish()) {
      Button publish = new Button("Publish…", VaadinIcon.UPLOAD.create(), e -> showPublishTab());
      draftBar.add(publish);
    }
  }
}
