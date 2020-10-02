#import "SNSMobileSdkCordovaPlugin.h"
#import <Cordova/CDV.h>
#import <IdensicMobileSDK/IdensicMobileSDK.h>

@interface SNSMobileSdkCordovaPlugin ()
@property (nonatomic, copy) void(^tokenExpirationOnComplete)(NSString * _Nullable newAccessToken);
@property (nonatomic, weak) SNSMobileSDK *sdk;
@end

@implementation SNSMobileSdkCordovaPlugin

- (void)launchSNSMobileSDK:(CDVInvokedUrlCommand *)command {
    
    NSDictionary *params = command.arguments.firstObject;
    
    if (![params isKindOfClass:NSDictionary.class]) {
        return [self complete:command withInvalidParameters:@"No params detected"];
    }
    
    SNSMobileSDK *sdk = [SNSMobileSDK setupWithBaseUrl:params[@"apiUrl"]
                                              flowName:params[@"flowName"]
                                           accessToken:params[@"accessToken"]
                                                locale:params[@"locale"]
                                          supportEmail:params[@"supportEmail"]];
    
    if (!sdk.isReady) {
        [self complete:command withSDK:sdk];
        return;
    }
    
    self.sdk = sdk;

    if ([params[@"debug"] boolValue]) {
        sdk.logLevel = SNSLogLevel_Debug;
    }
    
    __weak SNSMobileSdkCordovaPlugin *weakSelf = self;
    
    [sdk tokenExpirationHandler:^(void (^ _Nonnull onComplete)(NSString * _Nullable)) {
        
        weakSelf.tokenExpirationOnComplete = onComplete;
        
        [weakSelf.commandDelegate evalJs:@"SNSMobileSDK.getNewAccessToken();" scheduledOnRunLoop:NO];
    }];
    
    [sdk onDidDismiss:^(SNSMobileSDK * _Nonnull sdk) {
        
        [weakSelf complete:command withSDK:sdk];
    }];
    
    [self applyCustomizationIfAny];

    [sdk presentFrom:self.viewController];
}

- (void)setNewAccessToken:(CDVInvokedUrlCommand*)command {
    
    NSString *newAccessToken = command.arguments.firstObject;
    
//    NSLog(@"got new token: %@", newAccessToken);
    
    if (self.tokenExpirationOnComplete) {
        dispatch_async(dispatch_get_main_queue(), ^{
            self.tokenExpirationOnComplete(newAccessToken);
            self.tokenExpirationOnComplete = nil;
        });
    }
}

- (void)dismiss:(CDVInvokedUrlCommand*)command {

    [self.sdk dismiss];
}

#pragma mark - Customization

/**
 * Usage:
 *
 * Add a class named `IdensicMobileSDKCustomization` into the main project
 * and define a static method named `apply:` that will take an instance of `SNSMobileSDK`
 *
 * For example, in Swift:
 *
 * import IdensicMobileSDK
 *
 * class IdensicMobileSDKCustomization: NSObject {
 *
 *   @objc static func apply(_ sdk: SNSMobileSDK) {
 *   }
 * }
 *
 */
- (void)applyCustomizationIfAny {
    
    NSString *className = @"IdensicMobileSDKCustomization";
    
    Class customization = [NSBundle.mainBundle classNamed:className];
    if (!customization) {
        NSString *classPrefix = [NSBundle.mainBundle objectForInfoDictionaryKey:(__bridge NSString *)kCFBundleExecutableKey];
        if (classPrefix) {
            customization = [NSBundle.mainBundle classNamed:[NSString stringWithFormat:@"%@.%@", classPrefix, className]];
        }
    }
    
    if (customization && [customization respondsToSelector:@selector(apply:)]) {
        [customization performSelector:@selector(apply:) withObject:self.sdk];
    }
}

#pragma mark - Helpers

- (void)complete:(CDVInvokedUrlCommand *)command withSDK:(SNSMobileSDK *)sdk {

    NSMutableDictionary *result = NSMutableDictionary.new;
    
    result[@"success"] = @(sdk.status != SNSMobileSDKStatus_Failed);
    result[@"status"] = [sdk descriptionForStatus:sdk.status];
    
    if (sdk.status == SNSMobileSDKStatus_Failed) {
        result[@"errorType"] = [sdk descriptionForFailReason:sdk.failReason];
        result[@"errorMsg"] = sdk.verboseStatus;
    }
    
    [self complete:command withResult:result.copy];
}

- (void)complete:(CDVInvokedUrlCommand *)command withInvalidParameters:(NSString *)message {

    NSDictionary *result = @{
        @"success": @NO,
        @"status": @"Failed",
        @"errorType": @"InvalidParameters",
        @"errorMsg": message ?: @"",
    };
    
    [self complete:command withResult:result];
}

- (void)complete:(CDVInvokedUrlCommand *)command withResult:(NSDictionary *)result {
    
    CDVCommandStatus commandStatus = CDVCommandStatus_OK; //[result[@"success"] boolValue] ? CDVCommandStatus_OK : CDVCommandStatus_ERROR;
    
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:commandStatus
                                                  messageAsDictionary:result];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end

