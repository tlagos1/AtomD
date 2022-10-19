package com.sorbonne.atom_d.tools

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager


class MyAlertDialog: DialogFragment() {

    private val TAG = MyAlertDialog::class.simpleName

    enum class MESSAGE_TYPE {
        ALERT_INFO,
        ALERT_OPTION,
        ALERT_ON_HOLD
    }



    companion object {
        private var funcOk: Runnable? = null
        private var funcCancel: Runnable? = null

        private fun setfuncOk(func: Runnable?) {
            funcOk = func
        }

        private fun setfuncCancel(func: Runnable?) {
            funcCancel = func
        }

        fun newInstance(
            title: String?,
            message: String?,
            iconId: Int,
            isCancelable: Boolean,
            type: MESSAGE_TYPE?,
            funcA: Runnable?,
            funcB: Runnable?
        ): MyAlertDialog {
            val frag = MyAlertDialog()
            val args = Bundle()
            args.putString("title", title)
            args.putString("message", message)
            args.putInt("iconId", iconId)
            args.putBoolean("isCancelable", isCancelable)
            args.putSerializable("messageType", type)
            frag.arguments = args
            setfuncOk(funcA)
            setfuncCancel(funcB)
            return frag
        }


        fun showDialog(
            manager: FragmentManager?,
            TAG: String?,
            title: String?,
            message: String?,
            iconId: Int,
            isCancelable: Boolean,
            type: MESSAGE_TYPE?,
            functionOk: Runnable?,
            functionCancel: Runnable?
        ): DialogFragment {
            val newFragment: DialogFragment = newInstance(
                title,
                message,
                iconId,
                isCancelable,
                type,
                functionOk,
                functionCancel
            )
            newFragment.show(manager!!, TAG)
            return newFragment
        }
    }




    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val title = requireArguments().getString("title")
        val message = requireArguments().getString("message")
        val iconId = requireArguments().getInt("iconId")
        val isCancelable = requireArguments().getBoolean("isCancelable")
        val messageType = requireArguments().getSerializable("messageType")

        var newAlertDialog = AlertDialog.Builder(activity).create()

        if (MESSAGE_TYPE.ALERT_INFO == messageType) {
            newAlertDialog = infoMessage(title, message, iconId, funcOk)
        } else if (MESSAGE_TYPE.ALERT_OPTION == messageType) {
            newAlertDialog = optionMessage(
                title,
                message,
                iconId,
                funcOk,
                funcCancel
            )
        } else if (MESSAGE_TYPE.ALERT_ON_HOLD == messageType) {
            newAlertDialog = onHoldMessage(title, message, iconId)
        } else {
            Log.e(TAG, "Not valid Alert Dialog")
        }
        setCancelable(isCancelable)
        newAlertDialog.setCanceledOnTouchOutside(isCancelable)

        return newAlertDialog
    }

    fun infoMessage(title: String?, message: String?, iconId: Int, func: Runnable?): AlertDialog? {
        return AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setIcon(iconId)
            .setPositiveButton(
                android.R.string.ok
            ) { _: DialogInterface?, _: Int ->
                func?.run()
            }
            .create()
    }

    fun optionMessage(
        title: String?,
        message: String?,
        iconId: Int,
        funcOK: Runnable?,
        funcCancel: Runnable?
    ): AlertDialog? {
        return AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setIcon(iconId)
            .setPositiveButton(
                android.R.string.ok
            ) { _: DialogInterface?, _: Int ->
                funcOK?.run()
            }
            .setNegativeButton(
                android.R.string.cancel
            ) { _: DialogInterface?, _: Int ->
                funcCancel?.run()
            }
            .create()
    }

    fun onHoldMessage(title: String?, message: String?, iconId: Int): AlertDialog? {
        return AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setIcon(iconId)
            .create()
    }



}