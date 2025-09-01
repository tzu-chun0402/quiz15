package com.example.quiz15.vo;

import java.util.ArrayList;
import java.util.List;

import com.example.quiz15.constants.ConstantsMessage;

import jakarta.validation.constraints.Min;

// �� class �N���D�s���M�@���j�b�@�_
public class QuestionIdAnswerVo {

	@Min(value = 1, message = ConstantsMessage.QUESTION_ID_ERROR)
	private int questionId;

	// ���w�w�]��: answerList ���w�]�ȷ|�q null �ܦ� �Ū�List
	private List<String> answerList = new ArrayList<>();

	public int getQuestionId() {
		return questionId;
	}

	public void setQuestionId(int questionId) {
		this.questionId = questionId;
	}

	public List<String> getAnswerList() {
		return answerList;
	}

	public void setAnswerList(List<String> answerList) {
		this.answerList = answerList;
	}

}
