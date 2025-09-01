package com.example.quiz15.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.quiz15.entity.Question;
import com.example.quiz15.entity.QuestionId;

@Repository
public interface QuestionDao extends JpaRepository<Question, QuestionId> {

	@Modifying
	@Transactional
	@Query(value = "insert into question (quiz_id, question_id, question, " //
			+ " type, is_required, options) values (?1, ?2, ?3, ?4, ?5, ?6)", //
			nativeQuery = true)
	public void insert(int quizId, int questionId, String question, String type, //
			boolean isRequired, String options);
	
	@Modifying
	@Transactional
	@Query(value = "delete from question where quiz_id = ?1",
			nativeQuery = true)
	public void deleteByQuizId(int quizId);
	
	@Query(value = "select * from question where quiz_id = ?1",
			nativeQuery = true)
	public List<Question> getQuestionsByQuizId(int quizId);
}
