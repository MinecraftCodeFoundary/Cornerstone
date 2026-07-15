package org.yeyao.cornerstone.teleport;

import org.yeyao.cornerstone.data.StoredLocation;

/** Named server destination with an explicit visibility policy. */
public record Warp(String name, StoredLocation location, Access access) {
    public enum Access { PUBLIC, PERMISSION, ADMIN }
}
