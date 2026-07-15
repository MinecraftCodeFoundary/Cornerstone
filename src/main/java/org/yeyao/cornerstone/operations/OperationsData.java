package org.yeyao.cornerstone.operations;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

final class OperationsData {
    boolean maintenanceEnabled;
    final Set<UUID> maintenanceAllowlist = new LinkedHashSet<>();
}
