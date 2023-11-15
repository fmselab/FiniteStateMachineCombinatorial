package enums;

/**
 * Test generation mode
 */
public enum Mode {
	ONLY_CONSTRAINT, // Only valid constraint
	STANDARD_CIT, // Standard Combinatorial Testing
	TRANSITIONS_COVERAGE, // Covers all the transitions
	STATES_COVERAGE // Covers all the states
}