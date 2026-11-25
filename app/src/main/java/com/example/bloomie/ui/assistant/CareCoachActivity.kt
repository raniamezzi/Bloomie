package com.example.bloomie.ui.assistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.bloomie.R
import com.example.bloomie.data.db.CoachRepository
import com.example.bloomie.databinding.ActivityCareCoachBinding
import com.example.bloomie.databinding.ItemBulletLineBinding
import com.example.bloomie.databinding.ItemCoachCauseBinding
import com.example.bloomie.databinding.ItemCoachOptionBinding
import com.example.bloomie.databinding.ItemCoachPreventionBinding
import com.example.bloomie.databinding.ItemCoachQuestionBinding
import com.example.bloomie.models.CoachCause
import com.example.bloomie.models.CoachData
import com.example.bloomie.models.CoachOption
import com.example.bloomie.models.CoachRule

/**
 * Screen 12: a three-step troubleshooter driven by `assets/coach_rules.json`.
 *
 * Step 1 picks a symptom, step 2 asks its follow-up questions, and step 3 ranks the possible
 * causes. Each chosen answer adds one point to the causes it lists; the highest scores come first.
 */
class CareCoachActivity : AppCompatActivity() {

    private enum class Step(val number: Int) { SYMPTOM(1), QUESTIONS(2), RESULT(3) }

    private lateinit var binding: ActivityCareCoachBinding
    private lateinit var inflater: LayoutInflater

    private var data: CoachData? = null
    private var step: Step = Step.SYMPTOM
    private var symptom: CoachRule? = null

    /** Question id to the option the user picked for it. */
    private val answers = LinkedHashMap<String, CoachOption>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCareCoachBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inflater = LayoutInflater.from(this)

        binding.btnBack.setOnClickListener { goBack() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        })

        data = CoachRepository.load(this)
        if (data == null) {
            showLoadError()
            return
        }

        restoreState(savedInstanceState)
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_STEP, step.number)
        outState.putString(STATE_SYMPTOM, symptom?.id)
        outState.putStringArrayList(STATE_ANSWER_QUESTIONS, ArrayList(answers.keys))
        outState.putStringArrayList(STATE_ANSWER_OPTIONS, ArrayList(answers.values.map { it.id }))
    }

    private fun restoreState(saved: Bundle?) {
        if (saved == null) return
        val coachData = data ?: return

        symptom = coachData.symptoms.firstOrNull { it.id == saved.getString(STATE_SYMPTOM) }
        val questionIds = saved.getStringArrayList(STATE_ANSWER_QUESTIONS).orEmpty()
        val optionIds = saved.getStringArrayList(STATE_ANSWER_OPTIONS).orEmpty()
        val chosenSymptom = symptom
        if (chosenSymptom != null) {
            questionIds.zip(optionIds).forEach { (questionId, optionId) ->
                val option = chosenSymptom.questions
                    .firstOrNull { it.id == questionId }
                    ?.options
                    ?.firstOrNull { it.id == optionId }
                if (option != null) answers[questionId] = option
            }
        }

        val savedStep = saved.getInt(STATE_STEP, Step.SYMPTOM.number)
        step = if (chosenSymptom == null) {
            Step.SYMPTOM
        } else {
            Step.values().firstOrNull { it.number == savedStep } ?: Step.SYMPTOM
        }
        // A saved result is only valid when every question was answered.
        if (step == Step.RESULT && answers.size < chosenSymptom?.questions?.size ?: 0) {
            step = Step.QUESTIONS
        }
    }

    // ---------------------------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------------------------

    private fun goBack() {
        when (step) {
            Step.SYMPTOM -> finish()
            Step.QUESTIONS -> {
                answers.clear()
                symptom = null
                step = Step.SYMPTOM
                render()
            }
            Step.RESULT -> {
                step = Step.QUESTIONS
                render()
            }
        }
    }

    private fun startOver() {
        answers.clear()
        symptom = null
        step = Step.SYMPTOM
        render()
    }

    // ---------------------------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------------------------

    private fun render() {
        binding.contentContainer.removeAllViews()
        binding.txtError.visibility = View.GONE
        binding.txtStepCounter.text = getString(R.string.coach_step_of, step.number, STEP_COUNT)
        binding.progressStep.setProgressCompat(step.number, true)

        when (step) {
            Step.SYMPTOM -> renderSymptomStep()
            Step.QUESTIONS -> renderQuestionStep()
            Step.RESULT -> renderResultStep()
        }
        binding.scrollContent.scrollTo(0, 0)
    }

    private fun renderSymptomStep() {
        binding.txtStepTitle.setText(R.string.coach_title)
        binding.txtStepSubtitle.setText(R.string.coach_subtitle)
        binding.btnPrimary.visibility = View.GONE

        for (rule in data?.symptoms.orEmpty()) {
            val option = ItemCoachOptionBinding.inflate(inflater, binding.contentContainer, false)
            option.txtOptionTitle.text = rule.title
            option.txtOptionSubtitle.text = rule.description
            option.txtOptionSubtitle.visibility = if (rule.description.isBlank()) View.GONE else View.VISIBLE
            option.cardOption.setOnClickListener {
                symptom = rule
                answers.clear()
                step = Step.QUESTIONS
                render()
            }
            binding.contentContainer.addView(option.root)
        }
    }

    private fun renderQuestionStep() {
        val rule = symptom ?: return startOver()
        binding.txtStepTitle.setText(R.string.coach_step_questions)
        binding.txtStepSubtitle.text = rule.title

        for (question in rule.questions) {
            val block = ItemCoachQuestionBinding.inflate(inflater, binding.contentContainer, false)
            block.txtQuestion.text = question.text

            val cards = mutableListOf<Pair<CoachOption, ItemCoachOptionBinding>>()
            for (option in question.options) {
                val card = ItemCoachOptionBinding.inflate(inflater, block.optionsContainer, false)
                card.txtOptionTitle.text = option.label
                card.cardOption.setOnClickListener {
                    answers[question.id] = option
                    cards.forEach { (candidate, views) -> styleOption(views, candidate.id == option.id) }
                    updatePrimaryButton()
                }
                styleOption(card, answers[question.id]?.id == option.id)
                cards.add(option to card)
                block.optionsContainer.addView(card.root)
            }
            binding.contentContainer.addView(block.root)
        }

        binding.btnPrimary.visibility = View.VISIBLE
        binding.btnPrimary.setText(R.string.coach_see_diagnosis)
        binding.btnPrimary.setOnClickListener {
            if (allQuestionsAnswered()) {
                step = Step.RESULT
                render()
            }
        }
        updatePrimaryButton()
    }

    private fun renderResultStep() {
        val rule = symptom ?: return startOver()
        binding.txtStepTitle.setText(R.string.coach_step_diagnosis)
        binding.txtStepSubtitle.text = rule.title

        val ranked = rankCauses(rule)
        if (ranked.isEmpty()) {
            binding.txtError.setText(R.string.coach_no_causes)
            binding.txtError.visibility = View.VISIBLE
        }

        val questionCount = rule.questions.size.coerceAtLeast(1)
        ranked.take(MAX_CAUSES_SHOWN).forEachIndexed { index, (cause, score) ->
            val card = ItemCoachCauseBinding.inflate(inflater, binding.contentContainer, false)
            val isTop = index == 0

            card.txtCauseLabel.setText(if (isTop) R.string.coach_likely_cause else R.string.coach_other_causes)
            card.txtCauseTitle.text = cause.title
            card.txtMatch.text = getString(R.string.results_match, score * 100 / questionCount)
            card.txtExplanation.text = cause.explanation
            styleUrgency(card, cause)

            card.cardCause.strokeColor = getColor(if (isTop) R.color.sage_green else R.color.card_stroke)
            card.cardCause.strokeWidth =
                (resources.displayMetrics.density * if (isTop) 2 else 1).toInt()

            fillBullets(card.signsContainer, cause.signs, numbered = false)
            fillBullets(card.remediesContainer, cause.remedies, numbered = true)
            binding.contentContainer.addView(card.root)
        }

        if (rule.prevention.isNotBlank()) {
            val prevention = ItemCoachPreventionBinding.inflate(inflater, binding.contentContainer, false)
            prevention.txtPrevention.text = rule.prevention
            binding.contentContainer.addView(prevention.root)
        }

        binding.btnPrimary.visibility = View.VISIBLE
        binding.btnPrimary.isEnabled = true
        binding.btnPrimary.setText(R.string.coach_start_over)
        binding.btnPrimary.setOnClickListener { startOver() }
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    /** Causes with at least one point, highest score first; ties go to the more urgent cause. */
    private fun rankCauses(rule: CoachRule): List<Pair<CoachCause, Int>> {
        val scores = HashMap<String, Int>()
        for (option in answers.values) {
            for (causeId in option.causeIds) {
                scores[causeId] = (scores[causeId] ?: 0) + 1
            }
        }
        return rule.causes
            .filter { (scores[it.id] ?: 0) > 0 }
            .map { cause -> cause to (scores[cause.id] ?: 0) }
            .sortedWith(
                compareByDescending<Pair<CoachCause, Int>> { it.second }
                    .thenBy { urgencyRank(it.first.urgency) }
            )
    }

    private fun urgencyRank(urgency: String): Int = when (urgency.lowercase()) {
        "high" -> 0
        "medium" -> 1
        else -> 2
    }

    private fun styleUrgency(card: ItemCoachCauseBinding, cause: CoachCause) {
        val (textRes, background, color) = when (cause.urgency.lowercase()) {
            "high" -> Triple(R.string.urgency_high, R.drawable.bg_badge_overdue, R.color.status_overdue)
            "medium" -> Triple(R.string.urgency_medium, R.drawable.bg_badge_warning, R.color.status_warning)
            else -> Triple(R.string.urgency_low, R.drawable.bg_badge_healthy, R.color.status_healthy)
        }
        card.txtUrgency.setText(textRes)
        card.txtUrgency.setBackgroundResource(background)
        card.txtUrgency.setTextColor(getColor(color))
    }

    private fun fillBullets(container: ViewGroup, lines: List<String>, numbered: Boolean) {
        container.removeAllViews()
        lines.forEachIndexed { index, line ->
            val row = ItemBulletLineBinding.inflate(inflater, container, false)
            row.txtMarker.text = if (numbered) "${index + 1}." else "•"
            row.txtLine.text = line
            container.addView(row.root)
        }
    }

    private fun styleOption(option: ItemCoachOptionBinding, selected: Boolean) {
        option.cardOption.strokeColor = getColor(if (selected) R.color.sage_green else R.color.card_stroke)
        option.cardOption.strokeWidth = (resources.displayMetrics.density * if (selected) 2 else 1).toInt()
        option.cardOption.setCardBackgroundColor(
            getColor(if (selected) R.color.badge_mint else R.color.surface_white)
        )
    }

    private fun allQuestionsAnswered(): Boolean {
        val rule = symptom ?: return false
        return rule.questions.all { answers.containsKey(it.id) }
    }

    private fun updatePrimaryButton() {
        binding.btnPrimary.isEnabled = allQuestionsAnswered()
    }

    private fun showLoadError() {
        binding.txtStepTitle.setText(R.string.coach_title)
        binding.txtStepSubtitle.setText(R.string.coach_subtitle)
        binding.txtStepCounter.visibility = View.GONE
        binding.txtError.setText(R.string.coach_load_error)
        binding.txtError.visibility = View.VISIBLE
    }

    private companion object {
        const val STEP_COUNT = 3
        const val MAX_CAUSES_SHOWN = 3

        const val STATE_STEP = "state_step"
        const val STATE_SYMPTOM = "state_symptom"
        const val STATE_ANSWER_QUESTIONS = "state_answer_questions"
        const val STATE_ANSWER_OPTIONS = "state_answer_options"
    }
}
