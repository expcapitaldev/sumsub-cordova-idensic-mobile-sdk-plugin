#import <Cordova/CDV.h>

@interface SNSMobileSdkCordovaPlugin : CDVPlugin

- (void)launchSNSMobileSDK:(CDVInvokedUrlCommand*)command;
- (void)setNewAccessToken:(CDVInvokedUrlCommand*)command;
- (void)dismiss:(CDVInvokedUrlCommand*)command;

@end