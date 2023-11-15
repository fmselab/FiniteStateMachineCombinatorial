package enums;

/**
 * Test reparation mode
 */
public enum ReparationMode {
	REJECT_NOT_VALID, // Only valid sequences
	STOP_AT_ERROR, // When an error occurs, stop
	SKIP_ERROR // When an error occurs, jump to the next char
}