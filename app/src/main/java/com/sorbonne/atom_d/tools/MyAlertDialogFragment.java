package com.sorbonne.atom_d.tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;


public class MyAlertDialogFragment extends DialogFragment {

    String TAG = LogExporter.class.getSimpleName();

    public enum MESSAGE_TYPE  {
        ALERT_INFO,
        ALERT_OPTION,
        ALERT_ON_HOLD
    }

    private static Runnable funcOk;
    private static Runnable funcCancel;

    private static void setfuncOk(Runnable func){
        funcOk = func;
    }

    private static void setfuncCancel(Runnable func){
        funcCancel = func;
    }


    public static MyAlertDialogFragment newInstance( String title,
                                                     String message,
                                                     int iconId,
                                                     boolean isCancelable,
                                                     MESSAGE_TYPE type,
                                                     Runnable funcA,
                                                     Runnable funcB ) {
        MyAlertDialogFragment frag = new MyAlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putInt("iconId", iconId);
        args.putBoolean("isCancelable", isCancelable);
        args.putSerializable("messageType", type);

        frag.setArguments(args);

        setfuncOk(funcA);
        setfuncCancel(funcB);
        return frag;
    }


    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)  {

        assert getArguments() != null;
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");
        int iconId = getArguments().getInt("iconId");
        boolean isCancelable = getArguments().getBoolean("isCancelable");
        Serializable messageType = getArguments().getSerializable("messageType");

        AlertDialog newAlertDialog = new AlertDialog.Builder(getActivity()).create();

        if (MESSAGE_TYPE.ALERT_INFO.equals(messageType)) {
            newAlertDialog = infoMessage(title, message, iconId, funcOk);
        }else if(MESSAGE_TYPE.ALERT_OPTION.equals(messageType)){
            newAlertDialog = optionMessage(title, message, iconId, funcOk, funcCancel);
        }else if(MESSAGE_TYPE.ALERT_ON_HOLD.equals(messageType)){
            newAlertDialog = onHoldMessage(title, message, iconId);
        }else {
            Log.e(TAG, "Not valid Alert Dialog");
        }
        setCancelable(isCancelable);
        newAlertDialog.setCanceledOnTouchOutside(isCancelable);

        return newAlertDialog;
    }

    public AlertDialog infoMessage(String title, String message, int iconId, Runnable func) {
        return  new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setIcon(iconId)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if(func != null) {
                        func.run();
                    }
                })
                .create();
    }

    public AlertDialog optionMessage(String title, String message, int iconId, Runnable funcOK, Runnable funcCancel)
    {
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setIcon(iconId)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if(funcOK != null) {
                        funcOK.run();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    if(funcCancel != null) {
                        funcCancel.run();
                    }
                })
                .create();
    }

    public AlertDialog onHoldMessage(String title, String message, int iconId) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setIcon(iconId)
                .create();
    }

    public static DialogFragment showDialog(FragmentManager manager, String TAG, String title, String message, int iconId, boolean isCancelable, MyAlertDialogFragment.MESSAGE_TYPE type, Runnable functionOk, Runnable functionCancel) {

        DialogFragment newFragment = MyAlertDialogFragment.newInstance(
                title,
                message,
                iconId,
                isCancelable,
                type,
                functionOk,
                functionCancel);
        newFragment.show(manager, TAG);

        return newFragment;
    }

}
