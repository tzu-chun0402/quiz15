package com.example.quiz15.vo;

import java.time.LocalDate;
import java.util.List;

import com.example.quiz15.constants.ConstantsMessage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// 一個 QuizCreateReq 表示一張問卷
public class QuizCreateReq {

	// 不帶問卷的編號是因為問卷編號是自動生成的流水號

	@NotBlank(message = ConstantsMessage.QUIZ_NAME_ERROR)
	private String name;

	@NotBlank(message = ConstantsMessage.QUIZ_DESCRIPTION_ERROR)
	private String description;

	@NotNull(message = ConstantsMessage.QUIZ_START_DATE_ERROR)
	private LocalDate startDate;

	@NotNull(message = ConstantsMessage.QUIZ_END_DATE_ERROR)
	private LocalDate endDate;

	private boolean published;

	// 同一張問卷可能會有多個問題
	@Valid // 嵌套驗證: QuestionVo 也有使用 Validation 驗證，所以要加上 @Valid 才會使其生效
	@NotEmpty(message = ConstantsMessage.QUESTION_VO_ERROR)
	private List<QuestionVo> questionList;

	public QuizCreateReq() {
		super();
	}

	public QuizCreateReq(String name,
			String description,
			LocalDate startDate,
			LocalDate endDate, boolean published,
			List<QuestionVo> questionList) {
		super();
		this.name = name;
		this.description = description;
		this.startDate = startDate;
		this.endDate = endDate;
		this.published = published;
		this.questionList = questionList;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public List<QuestionVo> getQuestionList() {
		return questionList;
	}

	public void setQuestionList(List<QuestionVo> questionList) {
		this.questionList = questionList;
	}

}
