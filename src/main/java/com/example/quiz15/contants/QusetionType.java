package com.example.quiz15.contants;

public enum QusetionType {
	SINGLE("Single"), //
	MULTI("Multi"), //
	TEXT("Text");

	private String type;

	private QusetionType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
