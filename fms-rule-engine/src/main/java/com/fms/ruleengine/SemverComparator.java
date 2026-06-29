package com.fms.ruleengine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SemverComparator {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-.*)?$");

    private SemverComparator() {
    }

    static int compare(String left, String right) {
        int[] leftParts = parse(left);
        int[] rightParts = parse(right);
        for (int i = 0; i < 3; i++) {
            int diff = leftParts[i] - rightParts[i];
            if (diff != 0) {
                return Integer.compare(leftParts[i], rightParts[i]);
            }
        }
        return 0;
    }

    private static int[] parse(String version) {
        if (version == null || version.isBlank()) {
            return new int[] {0, 0, 0};
        }
        Matcher matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            return new int[] {0, 0, 0};
        }
        return new int[] {
            Integer.parseInt(matcher.group(1)),
            matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)),
            matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3))
        };
    }
}
