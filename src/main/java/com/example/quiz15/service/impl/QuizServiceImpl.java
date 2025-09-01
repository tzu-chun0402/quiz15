package com.example.quiz15.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quiz15.constants.QuestionType;
import com.example.quiz15.constants.ResCodeMessage;
import com.example.quiz15.dao.FillinDao;
import com.example.quiz15.dao.QuestionDao;
import com.example.quiz15.dao.QuizDao;
import com.example.quiz15.entity.Question;
import com.example.quiz15.entity.Quiz;
import com.example.quiz15.service.ifs.QuizService;
import com.example.quiz15.vo.BasicRes;
import com.example.quiz15.vo.FeedbackRes;
import com.example.quiz15.vo.FeedbackUserRes;
import com.example.quiz15.vo.FillinReq;
import com.example.quiz15.vo.OptionCountVo;
import com.example.quiz15.vo.QuestionAnswerDto;
import com.example.quiz15.vo.QuestionAnswerVo;
import com.example.quiz15.vo.QuestionIdAnswerVo;
import com.example.quiz15.vo.QuestionRes;
import com.example.quiz15.vo.QuestionVo;
import com.example.quiz15.vo.QuizCreateReq;
import com.example.quiz15.vo.QuizUpdateReq;
import com.example.quiz15.vo.SearchReq;
import com.example.quiz15.vo.SearchRes;
import com.example.quiz15.vo.StatisticsRes;
import com.example.quiz15.vo.StatisticsVo;
import com.example.quiz15.vo.UserVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class QuizServiceImpl implements QuizService {

	// ���� ���O(�� Json �榡)�P���󤧶����ഫ
	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private QuizDao quizDao;

	@Autowired
	private QuestionDao questionDao;

	@Autowired
	private FillinDao fillinDao;

	/**
	 * @throws Exception
	 * @Transactional: �ư�</br>
	 *                 1. ��@�Ӥ�k������h�� Dao
	 *                 ��(���άO�P�@�i��g�h�����)�A�o�ǩҦ���������ӳ��n��P�@�����欰�A
	 *                 �ҥH�o�Ǹ�ƭn�������g�J���\�A���M�N�����g�J����</br>
	 *                 2. @Transactional ���Ħ^�Ҫ����`�w�]�O
	 *                 RunTimeException�A�Y�o�ͪ����`���O RunTimeException
	 *                 �Ψ�l���O�����`�����A��ƬҤ��|�^�ҡA�]���Q�n���u�n�o�ͥ���@�ز��`�ɸ�Ƴ��n�i�H�^�ҡA�i�H
	 *                 �N @Transactional �����Ľd��q RunTimeException ������
	 *                 Exception
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes create(QuizCreateReq req) throws Exception {
		// �Ѽ��ˬd�w�z�L @Valid ���ҤF
		try {
			// �ˬd���: �ϥαư��k
			BasicRes checkRes = checkDate(req.getStartDate(), req.getEndDate());
			if (checkRes.getCode() != 200) { // ������ 200 ����ˬd�X�����~
				return checkRes;
			}
			// �s�W�ݨ�
			quizDao.insert(req.getName(), req.getDescription(), req.getStartDate(), //
					req.getEndDate(), req.isPublished());
			// �s�W���ݨ���A���o�ݨ��y����
			// ���M�]�� @Transactional �|���N��ƴ���(commit)�i��Ʈw�A����ڤWSQL�y�k�w�g���槹���A
			// �̵M�i�H���o��������
			int quizId = quizDao.getMaxQuizId();
			// �s�W���D
			// ���X�ݨ������Ҧ����D
			List<QuestionVo> questionVoList = req.getQuestionList();
			// �B�z�C�@�D���D
			for (QuestionVo vo : questionVoList) {
				// �ˬd�D�������P�ﶵ
				checkRes = checkQuestionType(vo);
				// �I�s��k checkQuestionType �o�쪺 res �Y�O null�A����ˬd���S���D�A
				// �]����k���ˬd��᳣̫�S���D�ɬO�^�� ���\
				if (checkRes.getCode() != 200) {
//					return checkRes;
					// �]���e���w�g����F quizDao.insert �F�A�ҥH�o��n�ߥX Exception
					// �~�|�� @Transactional �ͮ�
					throw new Exception(checkRes.getMessage());
				}
				// �]�� MySQL �S�� List ����Ʈ榡�A�ҥH�n�� options ��Ʈ榡 �q List<String> �ন String
				List<String> optionsList = vo.getOptions();
				String str = mapper.writeValueAsString(optionsList);
				// �n�O�o�]�w quizId
				questionDao.insert(quizId, vo.getQuestionId(), vo.getQuestion(), //
						vo.getType(), vo.isRequired(), str);
			}
			return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
					ResCodeMessage.SUCCESS.getMessage());
		} catch (Exception e) {
			// ���� return BasicRes �ӬO�n�N�o�ͪ����`�ߥX�h�A�o�� @Transaction �~�|�ͮ�
			throw e;
		}
	}

	private BasicRes checkQuestionType(QuestionVo vo) {
		// 1. �ˬd type �O�_�O�W�w������
		String type = vo.getType();
		// ���] �q vo ���X�� type ���ŦX�w�q��3���������䤤�@�ءA�N��^���~�T��
		if (!(type.equalsIgnoreCase(QuestionType.SINGLE.getType())//
				|| type.equalsIgnoreCase(QuestionType.MULTI.getType())//
				|| type.equalsIgnoreCase(QuestionType.TEXT.getType()))) {
			return new BasicRes(ResCodeMessage.QUESTION_TYPE_ERROR.getCode(), //
					ResCodeMessage.QUESTION_TYPE_ERROR.getMessage());
		}
		// 2. type �O���Φh�諸�ɭԡA�ﶵ(options)�ܤ֭n��2��
		// ���] type ������ TEXT --> �N��� type �O���Φh��
		if (!type.equalsIgnoreCase(QuestionType.TEXT.getType())) {
			// ���Φh��ɡA�ﶵ�ܤ֭n��2��
			if (vo.getOptions().size() < 2) {
				return new BasicRes(ResCodeMessage.OPTIONS_INSUFFICIENT.getCode(), //
						ResCodeMessage.OPTIONS_INSUFFICIENT.getMessage());
			}
		} else { // else --> type �O text --> �ﶵ���ӬO null �άO size = 0
			// ���] �ﶵ���O null �� �ﶵ�� List ������
			if (vo.getOptions() != null && vo.getOptions().size() > 0) {
				return new BasicRes(ResCodeMessage.TEXT_HAS_OPTIONS_ERROR.getCode(), //
						ResCodeMessage.TEXT_HAS_OPTIONS_ERROR.getMessage());
			}
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	private BasicRes checkDate(LocalDate startDate, LocalDate endDate) {
		// 1. �}�l�������񵲧������ 2. �}�l���������e�Ыت������
		// �P�_��: ���] �}�l����񵲧������ �� �}�l������e����� --> �^���~�T��
		// LocalDate.now() --> ���o��e�����
		if (startDate.isAfter(endDate) //
				|| startDate.isBefore(LocalDate.now())) {
			return new BasicRes(ResCodeMessage.DATE_FORMAT_ERROR.getCode(), //
					ResCodeMessage.DATE_FORMAT_ERROR.getMessage());
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	private BasicRes checkStatus(LocalDate startDate, boolean isPublished) {
		// ���\�ݨ��ק諸���A����: 1. �|���o�G 2.�w�o�G+�|���}�l
		// �̷ӤW������A�{�����g�k�p�U
		// if(!isPublished || (isPublished && startDate.isAfter(LocalDate.now())))
		// ���O�]��2�ӱ��󦡬O�� OR (||) �걵�A��ܥu�n�@�ӱ��󦨥ߴN�|��^���\
		// �]���u�n������� || �᭱�����󦡡A���t�� isPublished = true�A
		// �W���� if ���󦡥i�ק令�p�U
		if (!isPublished || startDate.isAfter(LocalDate.now())) {
			// ��^���\��ܰݨ����\�Q�ק�
			return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
					ResCodeMessage.SUCCESS.getMessage());
		}
		return new BasicRes(ResCodeMessage.QUIZ_CANNOT_BE_EDITED.getCode(), //
				ResCodeMessage.QUIZ_CANNOT_BE_EDITED.getMessage());
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes update(QuizUpdateReq req) throws Exception {
		// �Ѽ��ˬd�w�z�L @Valid ���ҤF

		// ��s�O��w�s�b�ݨ��i��ק�
		try {
			// 1. �ˬd quizId �O�_�s�b
			int quizId = req.getQuizId();
			// int count = quizDao.getCountByQuizId(quizId);
			// ���ϥ� count �ƦӬO���X�㵧��ƥD�n�O�]�������ٷ|�ϥΨ��Ʈw�������
			Quiz quiz = quizDao.getById(quizId);
			if (quiz == null) {
				return new BasicRes(ResCodeMessage.NOT_FOUND.getCode(), //
						ResCodeMessage.NOT_FOUND.getMessage());
			}
			// 2. �ˬd���
			BasicRes checkRes = checkDate(req.getStartDate(), req.getEndDate());
			if (checkRes.getCode() != 200) { // ������ 200 ����ˬd�X�����~
				return checkRes;
			}
			// 3. �ˬd�쥻���ݨ����A(������쪺�ȬO�s�b��DB��)�O�_�i�Q��s
			checkRes = checkStatus(quiz.getStartDate(), quiz.isPublished());
			if (checkRes.getCode() != 200) { // ������ 200 ��ܰݨ������\�Q��s
				return checkRes;
			}
			// 4. ��s�ݨ�
			int updateRes = quizDao.update(quizId, req.getName(), req.getDescription(), //
					req.getStartDate(), req.getEndDate(), req.isPublished());
			if (updateRes != 1) { // ��ܸ�ƨS��s���\
				return new BasicRes(ResCodeMessage.QUIZ_UPDATE_FAILED.getCode(), //
						ResCodeMessage.QUIZ_UPDATE_FAILED.getMessage());
			}
			// 5. �R���P�@�i�ݨ����Ҧ����D
			questionDao.deleteByQuizId(quizId);
			// 6. �ˬd���D
			List<QuestionVo> questionVoList = req.getQuestionList();
			for (QuestionVo vo : questionVoList) {
				// �ˬd�D�������P�ﶵ
				checkRes = checkQuestionType(vo);
				// ��k���ˬd��᳣̫�S���D�ɬO�^�� ���\
				if (checkRes.getCode() != 200) {
					// �]���e���w�g����F quizDao.insert �F�A�ҥH�o��n�ߥX Exception
					// �~�|�� @Transactional �ͮ�
					throw new Exception(checkRes.getMessage());
				}
				// �]�� MySQL �S�� List ����Ʈ榡�A�ҥH�n�� options ��Ʈ榡 �q List<String> �ন String
				List<String> optionsList = vo.getOptions();
				String str = mapper.writeValueAsString(optionsList);
				// �n�O�o�]�w quizId
				questionDao.insert(quizId, vo.getQuestionId(), vo.getQuestion(), //
						vo.getType(), vo.isRequired(), str);
			}
		} catch (Exception e) {
			// ���� return BasicRes �ӬO�n�N�o�ͪ����`�ߥX�h�A�o�� @Transaction �~�|�ͮ�
			throw e;
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	@Override
	public SearchRes getAllQuizs() {
		List<Quiz> list = quizDao.getAll();
		return new SearchRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), list);
	}

	@Override
	public QuestionRes getQuizsByQuizId(int quizId) {
		if (quizId <= 0) {
			return new QuestionRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		List<QuestionVo> questionVoList = new ArrayList<>();
		List<Question> list = questionDao.getQuestionsByQuizId(quizId);
		// ��C�D�ﶵ����ƫ��A�q String �ഫ�� List<String>
		for (Question item : list) {
			String str = item.getOptions();
			try {
				List<String> optionList = mapper.readValue(str, new TypeReference<>() {
				});
				// �N�qDB���o���C�@�����(Question item) ���C�����ȩ�� QuestionVo
				// ���A�H�K��^���ϥΪ�
				// Question �M QuestionVo ���t�O�b�� �ﶵ ����ƫ��A
				QuestionVo vo = new QuestionVo(item.getQuizId(), item.getQuestionId(), //
						item.getQuestion(), item.getType(), item.isRequired(), optionList);
				// ��C�� vo ��� questionVoList ��
				questionVoList.add(vo);
			} catch (Exception e) {
				// �o�䤣�g throw e �O�]������k���S���ϥ� @Transactional�A���v�T��^���G
				return new QuestionRes(ResCodeMessage.OPTIONS_TRANSFER_ERROR.getCode(), //
						ResCodeMessage.OPTIONS_TRANSFER_ERROR.getMessage());
			}
		}
		return new QuestionRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), questionVoList);
	}

	@Override
	public SearchRes search(SearchReq req) {
		// �ഫ req ����
		// �Y quizName �O null�A�ন�Ŧr��
		String quizName = req.getQuizName();
		if (quizName == null) {
			quizName = "";
		} else { // �h�l���A���ݭn�g�A�����F�z�ѤU����3���B��l�Ӽg
			quizName = quizName;
		}
		// 3���B��l
		// �榡: �ܼƦW�� = ����P�_�� ? �P�_�����G�� true �ɭn�ᤩ���� : �P�_�����G�� false
		// �ɭn�ᤩ����;
		quizName = quizName == null ? "" : quizName;
		// �W�����{���X�i�H�u�ΤU���@��Ө��o��
		String quizName1 = req.getQuizName() == null ? "" : req.getQuizName();
		// =========================================
		// �ഫ �}�l��� --> �Y�S�����}�l��� --> ���w�@�ӫܦ����ɶ�
		LocalDate startDate = req.getStartDate() == null ? LocalDate.of(1970, 1, 1) //
				: req.getStartDate();

		LocalDate endDate = req.getEndDate() == null ? LocalDate.of(2999, 12, 31) //
				: req.getEndDate();
		List<Quiz> list = new ArrayList<>();
		if (req.isPublished()) {
			list = quizDao.getAllPublished(quizName, startDate, endDate);
		} else {
			list = quizDao.getAll(quizName, startDate, endDate);
		}
		return new SearchRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), list);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes delete(int quizId) throws Exception {
		if (quizId <= 0) {
			return new BasicRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		Quiz quiz = quizDao.getById(quizId);
		// �n�P�_�O�_�� null�A�Y���P�_�B���o���ȬO null �ɡA����ϥΤ�k�|����
		if (quiz == null) {
			return new BasicRes(ResCodeMessage.NOT_FOUND.getCode(), //
					ResCodeMessage.NOT_FOUND.getMessage());
		}
		// �ˬd�ݨ����A�O�_�i�Q��s
		BasicRes checkRes = checkStatus(quiz.getStartDate(), quiz.isPublished());
		if (checkRes.getCode() != 200) { // ������ 200 ��ܰݨ������\�Q��s
			return checkRes;
		}
		try {
			quizDao.deleteById(quizId);
			questionDao.deleteByQuizId(quizId);
		} catch (Exception e) {
			// ���� return BasicRes �ӬO�n�N�o�ͪ����`�ߥX�h�A�o�� @Transaction �~�|�ͮ�
			throw e;
		}
		return null;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes fillin(FillinReq req) throws Exception {
		// �ˬd��g���ݨ�(quiz)
		// �ˬd 1. �O�_�w�o�� 2. ��U������O�_�i�H��g(��ѬO�_���� �}�l����M������� ����)
		int count = quizDao.selectCountById(req.getQuizId(), LocalDate.now());
		if (count != 1) {
			return new BasicRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		// �ˬd�@�� email ����A�g�P�@���ݨ�
		count = fillinDao.selectCountByQuizIdAndEmail(req.getQuizId(), req.getEmail());
		if (count != 0) {
			return new BasicRes(ResCodeMessage.EMAIL_DUPLICATED.getCode(), //
					ResCodeMessage.EMAIL_DUPLICATED.getMessage());
		}
		// �ˬd�D��
		// �ˬd 1. ������S������ 2. �������h�ӵ��� 3. ���׸�ﶵ�O�@��(���ץ����O�ﶵ���@)
		// ���o�@�i�ݨ����Ҧ��D��
		List<Question> questionList = questionDao.getQuestionsByQuizId(req.getQuizId());
		List<QuestionIdAnswerVo> questionAnswerVoList = req.getQuestionAnswerVoList();
		// �N���D�s���M�^���ഫ�� Map�A�N�O�N QuestionAnswerVo �̭���2���ݩ��ন Map
		Map<Integer, List<String>> answerMap = new HashMap<>();
		for (QuestionIdAnswerVo vo : questionAnswerVoList) {
			answerMap.put(vo.getQuestionId(), vo.getAnswerList());
		}
		// �ˬd�C�@�D
		for (Question question : questionList) {
			int questionId = question.getQuestionId();
			String type = question.getType();
			boolean required = question.isRequired();
			// 1. �ˬd������S������ --> ����� questionId �S���b answerMap �� key �̭�
			if (required && !answerMap.containsKey(questionId)) {
				return new BasicRes(ResCodeMessage.ANSWER_REQUIRED.getCode(), //
						ResCodeMessage.ANSWER_REQUIRED.getMessage());
			}
			// 2. �ˬd�������h�ӵ���
			if (type.equalsIgnoreCase(QuestionType.SINGLE.getType())) {
				List<String> answerList = answerMap.get(questionId);
				if (answerList.size() > 1) {
					return new BasicRes(ResCodeMessage.QUESTION_TYPE_IS_SINGLE.getCode(), //
							ResCodeMessage.QUESTION_TYPE_IS_SINGLE.getMessage());
				}
			}
			// ²���D�S���ﶵ�A���L���D
			if (type.equalsIgnoreCase(QuestionType.TEXT.getType())) {
				continue;
			}
			// 3. �����D�����׸�ﶵ�O�_�@��(���ץ����O�ﶵ���@)
			String optionsStr = question.getOptions();
			List<String> answerList = answerMap.get(questionId);
			for (String answer : answerList) {
				// �N�C�ӵ��פ��O�_�Q�]�t�b�ﶵ�r�ꤤ
				if (!optionsStr.contains(answer)) {
					return new BasicRes(ResCodeMessage.OPTION_ANSWER_MISMATCH.getCode(), //
							ResCodeMessage.OPTION_ANSWER_MISMATCH.getMessage());
				}
			}
		}
		// �s���: �@�D�s���@�����
		for (QuestionIdAnswerVo vo : questionAnswerVoList) {
			// �� answerList �ন�r�ꫬ�A
			try {
				String str = mapper.writeValueAsString(vo.getAnswerList());
				fillinDao.insert(req.getQuizId(), vo.getQuestionId(), req.getEmail(), //
						str, LocalDate.now());
			} catch (Exception e) {
				throw e;
			}
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	public BasicRes fillin_test(FillinReq req) {
		// �ˬd��g���ݨ�(quiz)
		// �ˬd 1. �O�_�w�o�� 2. ��U������O�_�i�H��g(��ѬO�_���� �}�l����M������� ����)
		int count = quizDao.selectCountById(req.getQuizId(), LocalDate.now());
		if (count != 1) {
			return new BasicRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		// �ˬd�D��
		// �ˬd 1. ������S������ 2. �������h�ӵ��� 3. ���׸�ﶵ�O�@��(���ץ����O�ﶵ���@)
		// ���o�@�i�ݨ����Ҧ��D��
		List<Question> questionList = questionDao.getQuestionsByQuizId(req.getQuizId());
		List<QuestionIdAnswerVo> questionAnswerVoList = req.getQuestionAnswerVoList();
		// questionAnswerVoList �� size �i��|�� questionList �� size
		// �֡A�]�����i��O�D����ӨS����
		// �]���n���D�C�@�D�O�_����B��h��A�o�ˤ~�ள��g�����רӤ��
		// --> �ҥH questionList �n���~�h�j��

		// ���⥲���D�� questionId ���@�� List ��
		List<Integer> questionIdList = new ArrayList<>();
		for (Question question : questionList) {
			if (question.isRequired()) {
				questionIdList.add(question.getQuestionId());
			}
		}

		for (Question question : questionList) {
			int questionId = question.getQuestionId();
			String type = question.getType();
			boolean required = question.isRequired();
			// ���D�O���� --> �ˬd VoList ���O�_�����D���s���s�b
			for (QuestionIdAnswerVo vo : questionAnswerVoList) {
				int voQuestionId = vo.getQuestionId();
				// ���D������D�ؽs�����]�t�b questionIdList�A�^�ǿ��~
				if (required && !questionIdList.contains(voQuestionId)) {
					return new BasicRes(ResCodeMessage.ANSWER_REQUIRED.getCode(), //
							ResCodeMessage.ANSWER_REQUIRED.getMessage());
				}

				List<String> answerList = vo.getAnswerList();
				// �ˬd�ۦP�� questionId�A���D�O������S������ --> �^�ǿ��~
				if (questionId == voQuestionId && required //
				// CollectionUtils.isEmpty ���P�_�� null
				// QuestionAnswerVo ���� answerList �����w�s���w�]�ȡA�S�� mapping ��ɷ|�O�@�ӪŪ�
				// List
						&& answerList.isEmpty()) {
					return new BasicRes(ResCodeMessage.ANSWER_REQUIRED.getCode(), //
							ResCodeMessage.ANSWER_REQUIRED.getMessage());
				}
				// �ˬd�ۦP�� questionId�A�������h�ӵ��� --> �^�ǿ��~
				if (questionId == voQuestionId && type == QuestionType.SINGLE.getType() //
						&& answerList.size() > 1) {
					return new BasicRes(ResCodeMessage.QUESTION_TYPE_IS_SINGLE.getCode(), //
							ResCodeMessage.QUESTION_TYPE_IS_SINGLE.getMessage());
				}
			}
		}
		return null;
	}

	@Override
	public FeedbackUserRes feedbackUserList(int quizId) {
		if (quizId <= 0) {
			return new FeedbackUserRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		List<UserVo> userVoList = fillinDao.selectUserVoList(quizId);
		return new FeedbackUserRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), quizId, userVoList);
	}

	@Override
	public FeedbackRes feedback(int quizId, String email) {
		if (quizId <= 0) {
			return new FeedbackRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		List<QuestionAnswerDto> dtoList = fillinDao.selectQuestionAnswerList(quizId, email);
		List<QuestionAnswerVo> voList = new ArrayList<>();
		// 將QuestionAnswerDto 轉成 QuestionAnswerVo
		for (QuestionAnswerDto dto : dtoList) {
			// 將 dto 中的 answerStr 轉換成List<String>
			try {
				List<String> answerList = mapper.readValue(dto.getAnswerStr(), new TypeReference<>() {
				});
				// 一對一將 dto 的資料設定到 vo 中
				QuestionAnswerVo vo = new QuestionAnswerVo(dto.getQuestionId(), //
						dto.getQuestion(), dto.getType(), dto.isRequired(), answerList);
				voList.add(vo);
			} catch (Exception e) {
				// 不需要 throw 因為沒有使用 @Transactional，就只有取資料而已，所以可以 return 自定義
				// �۩w�q�����~��T
				return new FeedbackRes(ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getCode(), //
						ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getMessage());
			}
		}
		return new FeedbackRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), voList);
	}

	@Override
	public StatisticsRes statistics(int quizId) {
		if (quizId <= 0) {
			return new StatisticsRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		// 1. 取得問題和作答的資料
		List<QuestionAnswerDto> dtoList = fillinDao.selectQuestionAnswerList(quizId);
		// 2. 建立 Map 蒐集 ""選擇題"" 相同題號的所有作答: 作答也是選項，只是 List 中的字串會重複
		// Map<問題編號, 所有作答>
		Map<Integer, List<String>> quIdAnswerMap = new HashMap<>();
		for (QuestionAnswerDto dto : dtoList) {
			// 跳過簡答題
			if (dto.getType().equalsIgnoreCase(QuestionType.TEXT.getType())) {
				continue;
			}
			// 將 dto 中的 answerStr 轉換成 List<String>
			try {
				List<String> answerList = mapper.readValue(dto.getAnswerStr(), new TypeReference<>() {
				});
				if (quIdAnswerMap.containsKey(dto.getQuestionId())) {
					// Map 的特性是有相同的 key，其對應的 value 會後蓋前: 所以 key 若已存在，不能直接 put 值
					// 若 quIdAnswerMap 中已存在 key，則把 key 對應的 value 取出後，與新的值相加
					List<String> oldList = quIdAnswerMap.get(dto.getQuestionId());
					// 把 answerList 加入到原本的 List 中
					oldList.addAll(answerList);
					// 再把新的 List 放回到 quIdAnswerMap 中
					quIdAnswerMap.put(dto.getQuestionId(), oldList);
				} else {
					// quIdAnswerMap 中不存在 key，直接把 key-value 放到 quIdAnswerMap 中
					quIdAnswerMap.put(dto.getQuestionId(), answerList);
				}
			} catch (Exception e) {
				// 不需要 throw 因為沒有使用 @Transactional，就只有取資料而已，所以可以 return 自定義
				return new StatisticsRes(ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getCode(), //
						ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getMessage());
			}
		}
		// 3.取得每題問題的選項
		List<Question> questionList = questionDao.getQuestionsByQuizId(quizId);
		// 建立題號和選項次數Vo陣列的 map: Map<題號, 選項次數VoList>
		Map<Integer, List<OptionCountVo>> quIdOptionVoListMap = new HashMap<>();
		for (Question question : questionList) {
			try {
				// 跳過 簡答題
				if (question.getType().equalsIgnoreCase(QuestionType.TEXT.getType())) {
					continue;
				}
				// 把選項字串轉換成 List
				List<String> optionList = mapper.readValue(question.getOptions(), new TypeReference<>() {
				});
				List<OptionCountVo> voList = new ArrayList<>();
				// 把選項 optionList 中的每個選項轉成 OptionCountVo
				for (String op : optionList) {
					// 每個 op 就是一個選項
					OptionCountVo vo = new OptionCountVo(op, 0);
					voList.add(vo);
				}
				// 把每題對應的選項 List 放到 map 中
				// 不需要判斷 quIdOptionListMap 的 key 是否已存在，因為相同問卷，
				// 其問題編號不會重複，也就是只會有一筆資料而已
				quIdOptionVoListMap.put(question.getQuestionId(), voList);
			} catch (Exception e) {
				return new StatisticsRes(ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getCode(), //
						ResCodeMessage.OBJECTMAPPER_PROCESSING_ERROR.getMessage());
			}
		}
		// 4. 計算每題每個選項的次數
		for (Entry<Integer, List<OptionCountVo>> map : quIdOptionVoListMap.entrySet()) {
			int questionId = map.getKey();
			List<OptionCountVo> voList = map.getValue();
			// 1. 從 quIdAnswerMap 中，取得相同 key 對應的 value
			List<String> answerList = quIdAnswerMap.get(questionId);
			for (OptionCountVo vo : voList) {
				// 紀錄 answerList 的 size，因為每次迴圈跑完 answerList 應該都會變更
				int size = answerList.size();
				String option = vo.getOption();
				// 移除所有 answerList 中的符合選項(option) --> 得到新的 answerList
				answerList.removeAll(List.of(option));
				// 紀錄新的 answerList 的 size
				int newSize = answerList.size();
				// 計算次數並把該值設定回 OptionCountVo 中
				int count = size - newSize;
				vo.setCount(count);
			}
		}
		// 5. 設定 res
		List<StatisticsVo> statisticsVoList = new ArrayList<>();
		for (Entry<Integer, List<OptionCountVo>> map : quIdOptionVoListMap.entrySet()) {
			// 將從 DB 取得的資料 set 到 vo
			for (QuestionAnswerDto dto : dtoList) {
				// 設定相同題號的資料
				if (map.getKey() == dto.getQuestionId()) {
					StatisticsVo vo = new StatisticsVo(dto.getQuestionId(), dto.getQuestion(), //
							dto.getType(), dto.isRequired(), map.getValue());
					statisticsVoList.add(vo);
				}
			}
		}
		return new StatisticsRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), statisticsVoList);
	}

}
