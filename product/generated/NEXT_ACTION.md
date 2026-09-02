# LibreCare — Next Actions (Product Discovery v2)

**Generated:** 2026-09-01  
**Status:** READY FOR IMPLEMENTATION

---

## SUMMARY

The automated Product Discovery pipeline has been corrected. The first review created artificial requirements from passing test evidence. This corrected review applies proper classification logic and identifies:

- **2 Validated Capabilities** (working, documented, no new requirements)
- **1 Test Coverage Gap** (validation research needed, not product features)
- **0 Candidate Requirements** (no genuine problems/opportunities identified)
- **5 Research-Focused Development Options** (uncertainty reduction activities)

---

## IMMEDIATE NEXT ACTIONS (Priority Order)

### Priority 1: Research — Stale Data Communication

**Objective:** Determine if seniors understand what "stale" means when glucose data is 30+ minutes old.

**Validated So Far:**
- Seniors can see freshness indicator and it's visible without sub-navigation

**Unknowns:**
- Does a 5-minute-old reading feel "fresh" or "stale" to seniors?
- At what age (threshold) does data feel concerning?
- Do seniors understand "Updated 30 min ago" means data is old?

**Recommended Test:**
1. Create scenario where sensor stops reporting for 30, 60, 90 minutes
2. Have senior assess the dashboard without guidance
3. Measure comprehension: "Is this data current?", "How old is this?", "Should I act on this?"
4. Record observations about confusion, clarity, or action (call caregiver?)

**Expected Outcome:**
- Either validates current indicator is sufficient, OR
- Reveals need for additional communication (warning, color-coding, aging indicator)

**Timeline:** 1-2 days of test design + execution

**Owner:** Product/Testing team

**Success Criteria:**
- Senior can accurately estimate data age
- Senior understands staleness implications
- Senior knows when to call caregiver

---

### Priority 2: Research — Caregiver Urgent Scenario

**Objective:** Test how caregivers respond when ONE person is high-risk among multiple monitored individuals.

**Validated So Far:**
- Basic person-switching is discoverable without navigation hints

**Unknowns:**
- Does person-switching remain discoverable under stress (alert for one person)?
- Do caregivers know how to respond to alerts?
- Do they know which person triggered the alert?
- Can they quickly switch context from normal to urgent?

**Recommended Test:**
1. Set up multi-person scenario (3 people, all normal glucose)
2. Trigger RAPID_FALL alert for one person
3. Measure caregiver response:
   - Time to identify which person is at risk
   - Time to access that person's details
   - Understanding of risk level
   - Action taken (call? open app? context capture?)
4. Test with different alert designs

**Expected Outcome:**
- Either validates current alerts + person-switching sufficient, OR
- Reveals need for:
  - More prominent alert wording
  - Forced context switching to alert person
  - Simplified action buttons (call caregiver, call clinician)

**Timeline:** 2-3 days of scenario design + execution

**Owner:** Product/Testing team

---

### Priority 3: Validation — Clinician Adverse Episode Test Suite

**Objective:** Build and execute test scenarios with diverse glucose patterns to verify clinician analytics trustworthiness.

**Validated So Far:**
- Analysis module is discoverable
- Clinical metrics (TIR, episodes, GMI, mean, variability) are displayable

**Unknowns:**
- Are TIR calculations accurate with diverse patterns?
- Does episode detection work reliably for hypo/hyper/mixed scenarios?
- Are metrics correctly calculated across edge cases?
- Can clinicians trust the analytics?

**Recommended Test Suite:**
1. **Scenario 1: Rapid Hypo**
   - 20+ minutes below 70 mg/dL
   - Verify: Low episode count, TIR reduction, mean glucose impact

2. **Scenario 2: Rapid Hyper**
   - 60+ minutes above 180 mg/dL
   - Verify: High episode count, TIR reduction, GMI impact

3. **Scenario 3: High Variability**
   - Multiple rapid excursions (spike-drop-spike pattern)
   - Verify: Episode counting, mean glucose accuracy, variability calculation

4. **Scenario 4: Mixed Episode**
   - Hypo (50 mg/dL) followed immediately by rebound hyper (300 mg/dL)
   - Verify: Correct counting, context preservation, pattern recognition

**Acceptance Criteria:**
- All metrics calculated correctly
- Clinician can understand the glucose story
- No calculation errors or false data
- Clinician reports confidence in analytics

**Timeline:** 3-5 days of test data prep + execution + validation

**Owner:** Clinical/Testing team

---

### Priority 4: Research — Senior Hypo Behavior Observation

**Objective:** Understand what seniors actually do when glucose is rapidly falling.

**Validated So Far:**
- Seniors can view current glucose and trend
- Seniors can see freshness indicator

**Unknowns:**
- Do seniors understand what RAPID_FALL trend means?
- What actions do seniors take? (call caregiver? eat carbs? rest?)
- What additional context would help seniors?
- Do seniors want context-capture (time, activity, stress)?

**Recommended Test:**
1. Create scenario with RAPID_FALL trend (e.g., 200 → 150 → 100 in 15 min)
2. Ask senior: "What's happening here? What should you do?"
3. Observe without guidance; record:
   - Time to comprehension
   - Confidence in understanding
   - Action chosen
   - Desire for additional context
4. Test with different alert designs / urgency signals

**Expected Outcome:**
- Validates seniors understand trend implications, OR
- Reveals need for:
  - Clearer trend explanations
  - Suggested actions ("Call caregiver", "Eat carbs")
  - Context-capture prompt (when did you last eat? exercise?)

**Timeline:** 2-3 days

**Owner:** Product/Testing team

---

### Priority 5: Research — Context Capture After Events

**Objective:** Determine if post-event context capture provides learning value.

**Unknowns:**
- Do users benefit from capturing meal/activity/stress after glucose events?
- What information is valuable? (exact meal, activity level, stress)
- When should context capture be triggered? (immediately? later?)
- Do users complete context capture or abandon it?

**Recommended Test:**
1. Design context-capture workflow:
   - Trigger: After significant glucose event (drop, spike, pattern)
   - Questions: What did you eat? What activity? Stress level?
   - Timing: Immediate vs. delayed vs. optional
2. Execute with real users over 1-2 weeks
3. Measure:
   - Adoption rate (% of events with context captured)
   - Completion rate (% of questions answered)
   - User perception ("Was this helpful?")
   - Learning value (did context inform future decisions?)

**Expected Outcome:**
- Either validates context capture is valuable feature, OR
- Shows it's low-priority / user-burden-heavy
- Informs if timing/questions should change

**Timeline:** 1 week experiment + analysis

**Owner:** Product/Research team

---

## RESEARCH WORKFLOW EXECUTION

### For Each Research Activity:

1. **Design** (define test, scenarios, success criteria)
2. **Prepare** (create test data, environment, user instructions)
3. **Execute** (run test, observe, record)
4. **Analyze** (classify evidence, measure outcomes, identify gaps)
5. **Document** (write findings, update VALIDATED_CAPABILITIES.md or TEST_RESEARCH_BACKLOG.yaml)
6. **Decide** (create requirement candidate only if genuine problem/opportunity found)

---

## CONSTRAINT: DO NOT CREATE REQUIREMENTS YET

These activities are **research, not feature implementation**. They answer "Should we build X?" not "Build X now."

Only if research reveals:
- User currently lacks a capability (PRODUCT_PROBLEM), OR
- User gains significant value from new capability (PRODUCT_OPPORTUNITY), OR
- Safety risk requires addressing (SAFETY_GAP)

...then create a requirement candidate for human decision.

---

## CURRENT BACKLOG STATUS

### product/review/REVIEW_QUEUE.yaml
**Status:** EMPTY (0 pending candidates)

**Reason:** Current evidence confirms existing capabilities work. No genuine problems/opportunities identified.

### product/research/TEST_RESEARCH_BACKLOG.yaml
**Status:** 1 item (Clinician adverse episode test coverage)

**Items:**
- TESTRUN-2026-003: Coverage Gap: Clinician — assess recent glucose control
  - Reason: Demo data lacks adverse scenarios
  - Status: OPEN
  - Priority: P1 (validation research)

### product/generated/VALIDATED_CAPABILITIES.md
**Status:** 2 capabilities documented

**Capabilities:**
- Caregiver multi-person switching
- Senior glucose/trend/freshness visibility

---

## SUCCESS METRICS

Research is successful when it:

1. ✓ Reduces uncertainty about user needs
2. ✓ Produces evidence (positive or negative) about a hypothesis
3. ✓ Leads to informed product decisions
4. ✓ Identifies genuine problems OR validates existing capabilities
5. ✓ Is documented in VALIDATED_CAPABILITIES.md or TEST_RESEARCH_BACKLOG.yaml

---

## DO NOT DO (Safety Guardrails)

❌ Do NOT automatically accept research findings as requirements  
❌ Do NOT create feature requirements from passing test results  
❌ Do NOT implement features without human decision (ACCEPT/HOLD/REJECT)  
❌ Do NOT mix testing activities with product requirements  
❌ Do NOT claim feature validation when testing coverage is incomplete  

✅ DO request human product review when research reveals genuine problems/opportunities  
✅ DO document all evidence for roadmap context  
✅ DO ask "Is this a problem users have?" before proposing features  
✅ DO focus on uncertainty reduction over feature generation  

---

## QUESTIONS FOR PRODUCT LEADERSHIP

1. **Which research activity is highest priority?**
   - Stale data communication (directly affects senior confidence)
   - Caregiver urgent scenario (affects multi-person workflows)
   - Clinician adverse episode validation (safety/trust)

2. **Which is blocking progress?**
   - Is incomplete clinician test coverage blocking release?
   - Does stale data communication need to be tested before next iteration?

3. **What timeline is acceptable?**
   - Can research complete in 1 week?
   - Can research extend over 2-3 weeks?

4. **Should research activities run in parallel or sequentially?**
   - If parallel: need more resources
   - If sequential: slower but focused

---

## EXPECTED OUTCOMES

After completing these 5 research activities:

- ✓ 3-5 genuine product requirement candidates identified (or none if no problems found)
- ✓ 2 existing validated capabilities confirmed with higher confidence
- ✓ Understanding of user gaps and needs
- ✓ Data-driven roadmap priorities
- ✓ Informed decisions for next iteration

**Result:** Product decisions based on evidence, not assumptions.

---

**For questions, contact:** Product team  
**Review frequency:** After each research activity  
**Next review date:** 2026-09-10 (or after first research completion)

