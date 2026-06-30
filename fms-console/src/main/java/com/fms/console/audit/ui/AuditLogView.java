package com.fms.console.audit.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "audit", layout = MainLayout.class)
@AnonymousAllowed
public class AuditLogView extends PlaceholderView {

  public AuditLogView() {
    super("Audit Log", "Read-only audit events with change diffs.");
  }
}
