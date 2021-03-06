package com.vmloft.develop.app.demo.call;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.exceptions.HyphenateException;
import com.vmloft.develop.app.demo.call.conference.ConferenceActivity;
import com.vmloft.develop.library.tools.VMActivity;
import com.vmloft.develop.library.tools.utils.VMLog;
import com.vmloft.develop.library.tools.utils.VMSPUtil;
import com.vmloft.develop.library.tools.widget.VMViewGroup;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * 音视频项目主类
 */
public class MainActivity extends VMActivity {

    @BindView(R.id.layout_root) View rootView;
    @BindView(R.id.view_group) VMViewGroup viewGroup;

    @BindView(R.id.edit_username) EditText usernameView;
    @BindView(R.id.edit_password) EditText passwordView;
    @BindView(R.id.edit_contacts_username) EditText contactsView;
    @BindView(R.id.text_info) TextView infoView;

    private String username;
    private String password;
    private String toUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(activity);

        init();
    }

    private void init() {
        username = (String) VMSPUtil.get("username", "");
        password = (String) VMSPUtil.get("password", "");
        toUsername = (String) VMSPUtil.get("toUsername", "");
        usernameView.setText(username);
        passwordView.setText(password);
        contactsView.setText(toUsername);

        String[] btnTitle = {"登录", "注册", "退出", "语音呼叫", "视频呼叫", "发起会议", "发送消息", "新版推送"};

        for (int i = 0; i < btnTitle.length; i++) {
            Button btn = new Button(new ContextThemeWrapper(activity, R.style.VMBtn_Green), null, 0);
            btn.setText(btnTitle[i]);
            btn.setId(100 + i);
            btn.setOnClickListener(viewListener);
            viewGroup.addView(btn);
        }

    }

    private View.OnClickListener viewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case 100:
                signIn();
                break;
            case 101:
                signUp();
                break;
            case 102:
                signOut();
                break;
            case 103:
                callVoice();
                break;
            case 104:
                callVideo();
                break;
            case 105:
                videoConference(true);
                break;
            case 106:
                sendMessage();
                break;
            case 107:
                sendNewPushMessage();
                break;
            }
        }
    };

    /**
     * 登录
     */
    private void signIn() {
        username = usernameView.getText().toString().trim();
        password = passwordView.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            printInfo("username or password null");
            return;
        }
        EMClient.getInstance().login(username, password, new EMCallBack() {
            @Override
            public void onSuccess() {
                VMLog.i("login success");

                VMSPUtil.put("username", username);
                VMSPUtil.put("password", password);
                printInfo("login success");
            }

            @Override
            public void onError(final int i, final String s) {
                String errorMsg = "login error: " + i + "; " + s;
                VMLog.i(errorMsg);
                printInfo(errorMsg);
            }

            @Override
            public void onProgress(int i, String s) {

            }
        });
    }

    /**
     * 注册账户
     */
    private void signUp() {
        username = usernameView.getText().toString().trim();
        password = passwordView.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            printInfo("username or password null");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().createAccount(username, password);
                } catch (HyphenateException e) {
                    String errorMsg = "sign up error " + e.getErrorCode() + "; " + e.getMessage();
                    VMLog.d(errorMsg);
                    printInfo(errorMsg);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 退出登录
     */
    private void signOut() {
        EMClient.getInstance().logout(EMClient.getInstance().isConnected(), new EMCallBack() {
            @Override
            public void onSuccess() {
                VMLog.i("logout success");
                printInfo("logout success");
            }

            @Override
            public void onError(int i, String s) {
                String errorMsg = "logout error: " + i + "; " + s;
                VMLog.i(errorMsg);
                printInfo(errorMsg);
            }

            @Override
            public void onProgress(int i, String s) {

            }
        });
    }

    /**
     * 视频呼叫
     */
    private void callVideo() {
        checkContacts();
        Intent intent = new Intent(MainActivity.this, VideoCallActivity.class);
        CallManager.getInstance().setChatId(toUsername);
        CallManager.getInstance().setInComingCall(false);
        CallManager.getInstance().setCallType(CallManager.CallType.VIDEO);
        startActivity(intent);
    }

    /**
     * 语音呼叫
     */
    private void callVoice() {
        checkContacts();
        Intent intent = new Intent(MainActivity.this, VoiceCallActivity.class);
        CallManager.getInstance().setChatId(toUsername);
        CallManager.getInstance().setInComingCall(false);
        CallManager.getInstance().setCallType(CallManager.CallType.VOICE);
        startActivity(intent);
    }

    private void checkContacts() {
        toUsername = contactsView.getText().toString().trim();
        if (toUsername.isEmpty()) {
            printInfo("contact user not null");
            return;
        }
        VMSPUtil.put("toUsername", toUsername);
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        checkContacts();
        EMMessage message = EMMessage.createTxtSendMessage("测试发送消息，主要是为了测试是否在线", toUsername);
        //设置强制推送
        message.setAttribute("em_force_notification", "true");
        //设置自定义推送提示
        JSONObject extObj = new JSONObject();
        try {
            extObj.put("em_push_title", "老版本推送显示内容");
            extObj.put("extern", "定义推送扩展内容");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        message.setAttribute("em_apns_ext", extObj);
        sendMessage(message);
    }

    /**
     * 发送新版推送消息
     */
    private void sendNewPushMessage() {
        checkContacts();
        EMMessage message = EMMessage.createTxtSendMessage("测试发送消息，主要是为了测试是否在线", toUsername);
        //设置强制推送
        message.setAttribute("em_force_notification", "true");
        //设置自定义推送提示
        JSONObject extObj = new JSONObject();
        try {
            extObj.put("em_push_name", "新版推送标题");
            extObj.put("em_push_content", "新版推送显示内容");
            extObj.put("extern", "定义推送扩展内容");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        message.setAttribute("em_apns_ext", extObj);
        sendMessage(message);
    }

    /**
     * 最终调用发送信息方法
     *
     * @param message 需要发送的消息
     */
    private void sendMessage(final EMMessage message) {
        /**
         *  调用sdk的消息发送方法发送消息，发送消息时要尽早的设置消息监听，防止消息状态已经回调，
         *  但是自己没有注册监听，导致检测不到消息状态的变化
         *  所以这里在发送之前先设置消息的状态回调
         */
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                String str = String.format("消息发送成功 msgId %s, content %s", message.getMsgId(), message
                        .getBody());
                VMLog.i(str);
                printInfo(str);
            }

            @Override
            public void onError(final int i, final String s) {
                String str = String.format("消息发送失败 code: %d, error: %s", i, s);
                VMLog.i(str);
                printInfo(str);
            }

            @Override
            public void onProgress(int i, String s) {
                // TODO 消息发送进度，这里不处理，留给消息Item自己去更新
                VMLog.i("消息发送中 progress: %d, %s", i, s);
            }
        });
        // 发送消息
        EMClient.getInstance().chatManager().sendMessage(message);
    }

    /**
     * 发起视频会议
     */
    private void videoConference(boolean isCreator) {
        Intent intent = new Intent(activity, ConferenceActivity.class);
        intent.putExtra("isCreator", isCreator);
        intent.putExtra("username", toUsername);
        onStartActivity(activity, intent);
    }

    private void printInfo(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoView.setText(infoView.getText().toString() + info + "\n");
            }
        });
    }
}
