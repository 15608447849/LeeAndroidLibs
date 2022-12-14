package lee.bottle.lib.toolset.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;

import java.lang.reflect.Field;

import lee.bottle.lib.toolset.R;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2018/4/17.
 * email: 793065165@qq.com
 */

public class DialogUtils {

    public interface Action0 {
        void onAction0();
    }

    public static AlertDialog dialogSimple(Context context,String title, String msg,String buttonText, final Action0 action0) {
        //弹出提示
        return dialogSimple(context,
                title,
                msg,
                buttonText,
                0,
                action0);
    }

    public static AlertDialog dialogSimple(Context context, String title,String msg,String buttonText,int token, final Action0 action0) {
        //弹出提示
        return build(context,
                title,
                msg,
                R.drawable.ic_warn,
                null == buttonText ? "确定" : buttonText,
                null,
                null,
                token,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (which == DialogInterface.BUTTON_POSITIVE){
                            if (action0!=null) action0.onAction0();
                        }
                    }
                });
    }

    public static AlertDialog dialogSimple2(Context context, String msg, String sureText,final Action0 sure,String cancelText,final Action0 cancel) {
        //弹出提示
        return build(context,
                "提示",
                msg,
                R.drawable.ic_warn,
                sureText==null?"确定":sureText,
                cancelText==null?"取消":cancelText,
                null,
                0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (which == DialogInterface.BUTTON_POSITIVE){
                            if (sure!=null) sure.onAction0();
                        }else if (which == DialogInterface.BUTTON_NEGATIVE){
                            if (cancel!=null) cancel.onAction0();
                        }
                    }
                });
    }

    public static AlertDialog build(Context context,
                             String title,
                             String message,
                             int iconRid,
                             String positiveText,
                             String negativeText,
                             String neutralText,
                             int token,
                             DialogInterface.OnClickListener listener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(title) ;//设置标题
        builder.setMessage(message) ;//设置内容

        builder.setIcon(iconRid);//设置图标，
        if (positiveText!=null){
            builder.setPositiveButton(positiveText,listener);
        }
        if (negativeText!=null){
            builder.setNegativeButton(negativeText,listener);
        }
        if (neutralText!=null){
            builder.setNeutralButton(neutralText,listener);
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        if (token!=0){
            dialog.getWindow().setType(token);
        }
        if (context instanceof Activity){
            if (!((Activity) context).isFinishing()){
                dialog.show();
            }
        }else{
            dialog.show();
        }

        return dialog;
    }

    public static void changeSystemDialogColor(AlertDialog dialog ,int titleColor,int contentColor,int positiveColor,int negativeColor,int neutralColor){
        if(dialog == null) return;
        if (titleColor!=0){
            TextView textView = getSystemDialogTextView(dialog ,"mTitleView");
            if (textView!=null) textView.setTextColor(titleColor);
        }
        if (contentColor!=0){
            TextView textView = getSystemDialogTextView(dialog ,"mMessageView");
            if (textView!=null) textView.setTextColor(titleColor);
        }
        if (positiveColor!=0){
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(positiveColor);
        }
        if (negativeColor!=0){
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(negativeColor);
        }
        if (neutralColor!=0){
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(neutralColor);
        }
    }

    private static TextView getSystemDialogTextView(AlertDialog dialog,String fieldName) {
        try {
            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(dialog);
            Field mField = mAlertController.getClass().getDeclaredField(fieldName);
            mField.setAccessible(true);
            return (TextView) mField.get(mAlertController);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            LLog.error(e);
        }
        return null;
    }

    public static ProgressDialog createSimpleProgressDialog(Context context, String message){
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    public static void createSimpleDateDialog(Context context, int y, int m, int d, DatePickerDialog.OnDateSetListener listener){
        new DatePickerDialog(context,listener,y,m,d).show();
    }

    public static void createSimpleListDialog(Context context,String title,CharSequence[] items,boolean autoDismiss,DialogInterface.OnClickListener listener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setItems(items,listener);
        if (!autoDismiss){
            builder.setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    closeDialog(dialog);
                    dialog.dismiss();
                }
            });
        }
        builder.setCancelable(autoDismiss);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (!autoDismiss) keepDialogOpen(dialog);
    }

    //保持dialog不关闭的方法
    public static void keepDialogOpen(Object dialog) {
        try {
            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(dialog, false);
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    //关闭dialog的方法
    public static void closeDialog(Object dialog) {
        try {
            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(dialog, true);
        } catch (Exception e) {
            LLog.error(e);
        }
    }

}
