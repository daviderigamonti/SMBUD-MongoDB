package com.smbud.app.db;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Queries {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String SSN_ESCAPE = "&SSN&";
    private static final String VDATE_ESCAPE = "&VDATE&";
    private static final String TDATE_ESCAPE = "&TDATE&";

    private static final String VERIFICATION_QUERY = "[{" +
            "'$match': {'person.ssn': '" + SSN_ESCAPE + "'}}, {" +
            "'$match': {'$expr': {'$or': " +
            "[{'$gte': [{'$dateFromString': {'dateString': '$vaccine.date', 'format': '%Y-%m-%d'}}, ISODate('" + VDATE_ESCAPE + "')]}, " +
            "{'$gte': [{'$dateFromString': {'dateString': '$test.date', 'format': '%Y-%m-%d'}}, ISODate('" + TDATE_ESCAPE + "')]}" +
            "]}}}]";
    private static final String VACCINE_VALIDITY_QUERY = "{}";
    private static final String TEST_VALIDITY_QUERY = "{}";

    private Queries() {}

    public static String buildVerificationQuery(String ssn, int vaccineValidityM, int testValidityH) {
        String vd = LocalDateTime.now().minusMonths(vaccineValidityM).toLocalDate().format(DTF);
        String td = LocalDateTime.now().minusHours(testValidityH).toLocalDate().format(DTF);

        return VERIFICATION_QUERY
                .replaceAll(SSN_ESCAPE, ssn)
                .replaceAll(VDATE_ESCAPE, vd)
                .replaceAll(TDATE_ESCAPE, td);
    }

    public static String buildVaccineValidityQuery() {
        return VACCINE_VALIDITY_QUERY;
    }

    public static String buildTestValidityQuery() {
        return  TEST_VALIDITY_QUERY;
    }
}
