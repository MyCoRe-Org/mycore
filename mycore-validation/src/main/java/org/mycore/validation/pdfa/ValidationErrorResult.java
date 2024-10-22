package org.mycore.validation.pdfa;


import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.profiles.ProfileDetails;
import org.verapdf.pdfa.validation.profiles.RuleId;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;
import org.verapdf.processor.reports.enums.JobEndStatus;
import java.util.HashMap;
import java.util.List;


public  class ValidationErrorResult implements ValidationResult {

    @Override
    public boolean isCompliant() {
        return false;
    }

    @Override
    public PDFAFlavour getPDFAFlavour() {
        return PDFAFlavour.NO_FLAVOUR;
    }

    @Override
    public ProfileDetails getProfileDetails() {
        return null;
    }

    @Override
    public int getTotalAssertions() {
        return 0;
    }

    @Override
    public List<TestAssertion> getTestAssertions() {
        return List.of();
    }

    @Override
    public ValidationProfile getValidationProfile() {
        return null;
    }

    @Override
    public JobEndStatus getJobEndStatus() {
        return JobEndStatus.CANCELLED;
    }

    @Override
    public HashMap<RuleId, Integer> getFailedChecks() {
        // Always return a failure with a generic "Unknown Error" RuleId
        HashMap<RuleId, Integer> failedChecks = new HashMap<>();
        RuleId unknownError = new RuleId() {
            @Override
            public PDFAFlavour.Specification getSpecification() {
                return PDFAFlavour.Specification.NO_STANDARD;
            }

            @Override
            public String getClause() {
                return "Validation error!";
            }

            @Override
            public int getTestNumber() {
                return 0;
            }
        };

        failedChecks.put(unknownError, 1);

        return failedChecks;
    }
}
