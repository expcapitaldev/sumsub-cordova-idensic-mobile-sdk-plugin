:warning: Changes marked in bold are possibly breaking. Please, pay special attention to them.

## [1.12.2] - 01.09.2020

- Support for Selfie with Document
- Validation for Phone, Email and Date of Birth has been added at the `APPLICANT_DATA` step
- If needed, general moderation comment will be shown when the applicant is rejected
- Support for `<b>` and `<i>` tags within `sns_step_*_prompt` strings
- An omission of the numeric prefix when there is the only step on the initial Status Screen
- Drawing of the `submitted` state when the liveness result is uncertain, but the applicant is allowed to proceed 
- Strings added: `sns_step_SELFIE_photo_title`, `sns_step_SELFIE_photo_brief`, `sns_step_SELFIE_photo_details`, `sns_liveness_check_submitted`
- Some small bugs fixed
- A way to customize iOS part with [the native means](https://developers.sumsub.com/msdk/ios/#customization) by an `IdensicMobileSDKCustomization` class that could be added into the iOS project:
```swift
import Foundation
import IdensicMobileSDK

class IdensicMobileSDKCustomization: NSObject {

  @objc static func apply(_ sdk: SNSMobileSDK) {

    sdk.theme.sns_StatusHeaderTitleColor = .red
  }
}
```

## [1.12.1] - 17.08.2020

- `dismiss()` method added to make SDK dismission easier
- Extended error handling upon uploading fail
- Added an ability to pick from the gallery on the `PROOF_OF_RESIDENCE` step

## [1.12.0] - 03.08.2020

- Support for `APPLICANT_DATA` step
- Ability to force locale with `withLocale` optional method

## [1.11.1] - 14.07.2020

- Making support email optional

## [1.11.0] - 07.07.2020

- **[applicant flows](https://test-api.sumsub.com/checkus#/sdkIntegrations/flows) based initialization (`flowName` mandatory parameter added)**
- **Translations are now managed from the [dashboard](https://test-api.sumsub.com/checkus#/sdkIntegrations/globalSettings/msdkI18n)**
- `.withDebug(boolean)` builder option added
- `.withSupportEmail(string)` builder option added
- Various security related improvements

## [1.10.3] - 17.06.2020

* Initial release

