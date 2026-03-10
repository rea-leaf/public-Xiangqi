package com.chf.chess.controller.support;

import com.chf.chess.license.LicenseManager;
import com.chf.chess.license.LicensedFeature;

/**
 * 授权门禁层，统一处理功能校验与 UI 同步。
 */
public final class FeatureGateSupport {

    private final LicenseManager licenseManager = LicenseManager.getInstance();

    public boolean require(LicensedFeature feature) {
        if (licenseManager.isFeatureEnabled(feature)) {
            return true;
        }
        licenseManager.showFeatureBlocked(feature);
        return false;
    }

    public void sync(View view) {
        licenseManager.reload();

        boolean engineEnabled = licenseManager.isFeatureEnabled(LicensedFeature.ENGINE);
        boolean analysisEnabled = licenseManager.isFeatureEnabled(LicensedFeature.ANALYSIS);
        boolean openingBookEnabled = licenseManager.isFeatureEnabled(LicensedFeature.OPENING_BOOK);
        boolean linkEnabled = licenseManager.isFeatureEnabled(LicensedFeature.LINK);
        boolean moveVoiceEnabled = licenseManager.isFeatureEnabled(LicensedFeature.MOVE_VOICE);
        boolean manualScoreEnabled = licenseManager.isFeatureEnabled(LicensedFeature.MANUAL_SCORE);

        view.setEngineControlsEnabled(engineEnabled);
        view.setAnalysisEnabled(analysisEnabled);
        view.setOpeningBookEnabled(openingBookEnabled);
        view.setLinkEnabled(linkEnabled);
        view.setManualScoreEnabled(manualScoreEnabled);

        if (!moveVoiceEnabled) {
            view.disableMoveVoice();
        }
        if (!openingBookEnabled) {
            view.disableOpeningBook();
        } else {
            view.enableOpeningBookFromConfig();
        }
        if (!engineEnabled) {
            view.disposeEngine();
        }
        if (!linkEnabled) {
            view.stopLinkMode();
        }
    }

    public interface View {
        void setEngineControlsEnabled(boolean enabled);

        void setAnalysisEnabled(boolean enabled);

        void setOpeningBookEnabled(boolean enabled);

        void setLinkEnabled(boolean enabled);

        void setManualScoreEnabled(boolean enabled);

        void disableMoveVoice();

        void disableOpeningBook();

        void enableOpeningBookFromConfig();

        void disposeEngine();

        void stopLinkMode();
    }
}
