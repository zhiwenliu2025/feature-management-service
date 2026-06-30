package com.fms.console.flag.ui;

import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.PlaceholderView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "flags", layout = MainLayout.class)
@AnonymousAllowed
public class FlagListView extends PlaceholderView {

  public FlagListView() {
    super("Feature Flags", "Searchable grid of flags with status filters and cursor pagination.");
  }
}
