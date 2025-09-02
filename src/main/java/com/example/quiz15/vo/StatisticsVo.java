package com.example.quiz15.vo;

import java.util.List;

public class StatisticsVo {

	private int questionId;
	
	private String question;

	private String type;

	private boolean required;

	private List<OptionCountVo> optionCountVoList;
	
	private List<String> answers;

	public StatisticsVo() {
		super();
	}

	public StatisticsVo(int questionId, String question, String type, boolean required,
			List<OptionCountVo> optionCountVoList, List<String> answers) {
		super();
		this.questionId = questionId;
		this.question = question;
		this.type = type;
		this.required = required;
		this.optionCountVoList = optionCountVoList;
		this.answers = answers;
	}

	public int getQuestionId() {
		return questionId;
	}

	public String getQuestion() {
		return question;
	}

	public String getType() {
		return type;
	}

	public boolean isRequired() {
		return required;
	}

	public List<OptionCountVo> getOptionCountVoList() {
		return optionCountVoList;
	}
	
	public List<String> getAnswers() {
        return answers;
    }

}
