package de.cotto.lndmanagej.controller.dto;

import de.cotto.lndmanagej.model.Pubkey;

import java.util.Set;

public record NodeWithWarningsDto(Set<String> warnings, String alias, Pubkey pubkey) {
}
