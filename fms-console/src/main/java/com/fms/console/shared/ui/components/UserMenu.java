package com.fms.console.shared.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

public class UserMenu extends MenuBar {

  public static final String COLOR_SCHEME_KEY = "fms.colorScheme";

  public UserMenu() {
    String displayName = resolveDisplayName();
    MenuItem userItem = addItem(displayName);
    var subMenu = userItem.getSubMenu();

    String roles = String.join(" · ", resolveRoles());
    if (roles.isBlank()) {
      roles = "local";
    }
    subMenu.addItem("Roles: " + roles).setEnabled(false);

    subMenu.addItem("Color: System", e -> setContentColorScheme("system"));
    subMenu.addItem("Color: Light", e -> setContentColorScheme("light"));
    subMenu.addItem("Color: Dark", e -> setContentColorScheme("dark"));

    subMenu.addItem("Sign out", e -> UI.getCurrent().getPage().setLocation("/logout"));

    applyStoredColorScheme();
  }

  private String resolveDisplayName() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return "User";
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof OidcUser oidc) {
      return oidc.getFullName() != null ? oidc.getFullName() : oidc.getEmail();
    }
    if (principal instanceof Jwt jwt) {
      String email = jwt.getClaimAsString("email");
      if (email != null) {
        return email;
      }
      return jwt.getSubject();
    }
    return auth.getName();
  }

  private java.util.List<String> resolveRoles() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return java.util.List.of();
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a.startsWith("ROLE_") || a.startsWith("SCOPE_"))
        .map(a -> a.replace("ROLE_", "").replace("SCOPE_", ""))
        .distinct()
        .toList();
  }

  private void applyStoredColorScheme() {
    Object stored = VaadinSession.getCurrent().getAttribute(COLOR_SCHEME_KEY);
    if (stored != null) {
      setContentColorScheme(stored.toString());
    }
  }

  private void setContentColorScheme(String scheme) {
    VaadinSession.getCurrent().setAttribute(COLOR_SCHEME_KEY, scheme);
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    String contentScheme = switch (scheme) {
      case "light" -> "light";
      case "dark" -> "dark";
      default -> "light dark";
    };
    ui.getElement().executeJs(
        "document.documentElement.style.setProperty('--aura-content-color-scheme', $0)", contentScheme);
    FmsNotification.info("Color scheme: " + scheme);
  }
}
