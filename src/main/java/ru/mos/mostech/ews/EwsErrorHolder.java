package ru.mos.mostech.ews;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class EwsErrorHolder {

	@Getter
	private static final List<String> errors = new ArrayList<>();

	private EwsErrorHolder() {
	}

	public static void addError(String error) {
		errors.add(error);
	}

	public static boolean hasErrors() {
		return !errors.isEmpty();
	}

}
