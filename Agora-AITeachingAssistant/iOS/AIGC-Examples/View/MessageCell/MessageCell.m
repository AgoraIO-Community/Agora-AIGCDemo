//
//  MessageCell.m
//  VideoConference
//
//  Created by SRS on 2020/5/13.
//  Copyright © 2020 agora. All rights reserved.
//

#import "MessageCell.h"
#import "UIColor+Addition.h"

@interface MessageCell()

@property (weak, nonatomic) IBOutlet UILabel *time;
@property (weak, nonatomic) IBOutlet UILabel *name;
@property (weak, nonatomic) IBOutlet UILabel *msg;
@property (weak, nonatomic) IBOutlet UILabel *translateLabel;
@property (weak, nonatomic) IBOutlet UIImageView *bgImgView;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *timeHeightConstraint;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *textBgWConstraint;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *msgTextConstraint;
@property (weak, nonatomic) IBOutlet UIButton *tipButton;
@property (weak, nonatomic) IBOutlet UIButton *translateButton;

@end

@implementation MessageCell

- (void)awakeFromNib {
    [super awakeFromNib];

    CGFloat top = self.bgImgView.image.size.height / 2.0;
    CGFloat left = self.bgImgView.image.size.width / 2.0;
    CGFloat bottom = self.bgImgView.image.size.height / 2.0;
    CGFloat right = self.bgImgView.image.size.width / 2.0;
    self.bgImgView.image = [self.bgImgView.image resizableImageWithCapInsets:UIEdgeInsetsMake(top, left, bottom, right)resizingMode:UIImageResizingModeStretch];
    
    [_tipButton setTitleColor:UIColor.redColor forState:UIControlStateHighlighted];
    _tipButton.showsTouchWhenHighlighted = YES;
    [_translateButton setTitleColor:UIColor.redColor forState:UIControlStateHighlighted];
    _translateButton.showsTouchWhenHighlighted = YES;
}

- (void)updateWithTime:(NSInteger)time
               message:(NSString *)msg
              username:(NSString *)username
        translatedText:(nullable NSString *)translatedText {
    self.msg.textColor = [UIColor colorWithHex:0x2E3848];
    self.msg.text = msg;
    self.name.text = [username stringByAppendingString:@":"];
    [_translateLabel setHidden:translatedText == nil];
    _translateLabel.text = translatedText;
    
    CGFloat maxWidth = UIScreen.mainScreen.bounds.size.width - 9 - 54 - 24;
    CGSize msgSize1 = [self.msg sizeThatFits:CGSizeMake(maxWidth, NSIntegerMax)];
    CGSize msgSize2 = [self.name sizeThatFits:CGSizeMake(maxWidth, NSIntegerMax)];
    CGFloat wConstraint = MAX(msgSize1.width, msgSize2.width);
    self.textBgWConstraint.constant = wConstraint + 24;

    if(time == 0){
        self.time.hidden = YES;
        self.timeHeightConstraint.constant = 0;
    } else {
        self.time.hidden = NO;
        self.timeHeightConstraint.constant = 17;
        
        NSDate *date = [[NSDate alloc]initWithTimeIntervalSince1970:time];
        NSDateFormatter *formatter = [[NSDateFormatter alloc]init];
        [formatter setDateFormat:@"hh:mm a"];
        NSString *timeString = [formatter stringFromDate:date];
        self.time.text = timeString;
    }
}

- (void)updateTimeShow:(BOOL)show {
    [self.time setHidden:!show];
    _msgTextConstraint.constant = show ? 35 : 10;
    [self.contentView layoutIfNeeded];
}

- (void)updateRightButtonShow:(BOOL)show {
    [_tipButton setHidden:!show];
}

- (IBAction)buttonTap:(UIButton *)sender {
    MessageCellActionType type = sender == _tipButton ? MessageCellActionTypeTip : MessageCellActionTypeTranslate;
    [_delegate respondsToSelector:@selector(messageCelldidTapButton:atIndexPath:)] ? [_delegate messageCelldidTapButton:type atIndexPath:_indexPath] : nil;
}
@end
