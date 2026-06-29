package com.fms.repository;

import java.time.Instant;
import java.util.List;

public interface PublishJobRepositoryCustom {

    List<Long> claimJobIds(String workerId, Instant now, int limit);
}
