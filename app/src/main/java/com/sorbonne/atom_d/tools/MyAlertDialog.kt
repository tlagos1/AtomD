package com.sorbonne.atom_d.tools

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.double_column.AdapterDoubleColumn


class MyAlertDialog: DialogFragment() {

    private val TAG = MyAlertDialog::class.simpleName

    enum class MessageType {
        ALERT_INFO,
        ALERT_ACCEPT_CANCEL,
        ALERT_OPTION,
        ALERT_ON_HOLD,
        ALERT_INPUT_TEXT,
        ALERT_INPUT_RECYCLE_VIEW
    }

    companion object {
        private var adapterDoubleColumn:AdapterDoubleColumn ?= null

        private var option1: ((Any?) -> Unit?)? = null
        private var option2: (() -> Unit?)? = null

        private fun setOption1(option: ((Any?) -> Unit?)?) {
            option1 = option
        }

        private fun setOption2(option: (() -> Unit?)?) {
            option2 = option
        }

        private fun newInstance(type: MessageType, iconId: Int, title: String,
                                message: String?, viewId: Int, isCancelable: Boolean,
                                option1: ((Any?) -> Unit?)?, option2: (() -> Unit?)?,
                                option1Text:String?, option2Text:String?, filter: String?,
                                adapterDoubleColumn: AdapterDoubleColumn?
        ): MyAlertDialog{
            val dialog = MyAlertDialog()
            val args = Bundle()
            args.putString("title", title)
            args.putString("message", message)
            args.putString("option1Text", option1Text)
            args.putString("option2Text", option2Text)
            args.putInt("viewId", viewId)
            args.putInt("iconId", iconId)
            args.putBoolean("isCancelable", isCancelable)
            args.putSerializable("messageType", type)
            args.putString("filter", filter)
            dialog.arguments = args
            this.adapterDoubleColumn = adapterDoubleColumn
            setOption1(option1)
            setOption2(option2)

            return dialog
        }

        fun showDialog(
            manager: FragmentManager, tag: String, type: MessageType, iconId: Int, title: String,
            message: String? = null, view: Int = -1, isCancelable: Boolean = false,
            option1: ((Any?) -> Unit?)? = null, option2: (() -> Unit?)? = null,
            option1Text:String? = null, option2Text:String? = null, filter: String? = null,
            adapterDoubleColumn: AdapterDoubleColumn? = null
        ): DialogFragment {
            val newFragment: DialogFragment =
                newInstance(
                    type, iconId, title, message, view, isCancelable,
                    option1, option2, option1Text, option2Text, filter,
                    adapterDoubleColumn
                )
            newFragment.show(manager, tag)
            return newFragment
        }
    }

    private fun infoMessage(title: String?, message: String?, viewId: Int, iconId: Int): AlertDialog {
        val alertDialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setIcon(iconId)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                option1?.let { it(null) }
            }
        val inflater = requireActivity().layoutInflater

        if(viewId >= 0){
            alertDialog.setView(inflater.inflate(viewId, null))
        } else {
            alertDialog.setMessage(message)
        }
        return alertDialog.create()
    }

    private fun optionMessage(title: String?, message: String?, viewId: Int, iconId: Int, option1Text: String?, option2Text: String?): AlertDialog {
        val mOption1Text = option1Text ?: resources.getString(android.R.string.ok)
        val mOption2Text = option2Text ?: resources.getString(android.R.string.cancel)

        val alertDialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setIcon(iconId)
            .setPositiveButton(mOption1Text) { _: DialogInterface?, _: Int ->
                option1?.let { it(null) }
            }
            .setNegativeButton(mOption2Text) { _: DialogInterface?, _: Int ->
                option2?.let { it() }
            }
        val inflater = requireActivity().layoutInflater

        if(viewId >= 0){
            alertDialog.setView(inflater.inflate(viewId, null))
        } else {
            alertDialog.setMessage(message)
        }
        return alertDialog.create()
    }

    private fun optionInputMessage(title: String?, iconId: Int, filter: String?): AlertDialog {
        val inflater = requireActivity().layoutInflater
        val myView = inflater.inflate(R.layout.dialog_text_input, null)
        if(filter == "Ipv4"){
            val input: EditText  = myView.findViewById(R.id.text_input)
            // TODO Filters
        }
        val alertDialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setIcon(iconId)
            .setView(myView)
            .setPositiveButton(android.R.string.ok,
                DialogInterface.OnClickListener { dialog, id ->
                    val input: EditText  = (dialog as AlertDialog).findViewById(R.id.text_input)
                    option1?.let { it(input)}
                })
            .setNegativeButton(android.R.string.cancel,
                DialogInterface.OnClickListener { dialog, id ->
                    getDialog()?.cancel()
                })
        return alertDialog.create()
    }

    private fun optionRecycleView(title: String?, iconId: Int): AlertDialog {
        val inflater = requireActivity().layoutInflater
        val myView = inflater.inflate(R.layout.dialog_recycleview ,null)

        CustomRecyclerView(
            requireContext(),
            myView.findViewById(R.id.input_list),
            adapterDoubleColumn!!,
            CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
        ).getRecyclerView()

        val alertDialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setIcon(iconId)
            .setView(myView)
            .setNeutralButton(android.R.string.ok) { dialog, which ->
                option1?.let {
                    it(adapterDoubleColumn)
                }
            }
        return alertDialog.create()
    }

    private fun onHoldMessage(title: String?, message: String?, viewId: Int, iconId: Int): AlertDialog {
        val alertDialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setIcon(iconId)
        val inflater = requireActivity().layoutInflater

        if(viewId >= 0){
            alertDialog.setView(inflater.inflate(viewId, null))
        } else {
            alertDialog.setMessage(message)
        }
        return alertDialog.create()
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val newAlertDialog: AlertDialog

        val title = requireArguments().getString("title")
        val message = requireArguments().getString("message")
        val viewId = requireArguments().getInt("viewId")
        val iconId = requireArguments().getInt("iconId")
        val isCancelable = requireArguments().getBoolean("isCancelable")
        val messageType = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            requireArguments().getSerializable("messageType", MessageType::class.java)!!
        }else{
            requireArguments().getSerializable("messageType")!!
        }
        val option1Text = requireArguments().getString("option1Text")
        val option2Text = requireArguments().getString("option2Text")
        val filter = requireArguments().getString("filter")

        when(messageType){
            MessageType.ALERT_INFO -> {
                newAlertDialog = infoMessage(title, message, viewId, iconId)
            }
            MessageType.ALERT_ACCEPT_CANCEL -> {
                newAlertDialog = optionMessage(title, message, viewId, iconId, null, null)
            }
            MessageType.ALERT_ON_HOLD -> {
                newAlertDialog = onHoldMessage(title, message, viewId, iconId)
            }
            MessageType.ALERT_OPTION -> {
                newAlertDialog = optionMessage(title, message, viewId, iconId, option1Text, option2Text)
            }
            MessageType.ALERT_INPUT_TEXT -> {
                newAlertDialog = optionInputMessage(title, iconId, filter)
            }
            MessageType.ALERT_INPUT_RECYCLE_VIEW -> {
                newAlertDialog = optionRecycleView(title,iconId)
            }
            else -> {
               throw  java.lang.IllegalStateException("unknown alertDialog type")
            }
        }
        setCancelable(isCancelable)
        newAlertDialog.setCanceledOnTouchOutside(isCancelable)
        return newAlertDialog
    }

//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        val messageType = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
//            requireArguments().getSerializable("messageType", MessageType::class.java)!!
//        }else{
//            requireArguments().getSerializable("messageType")!!
//        }
//        return if(messageType == MessageType.ALERT_INPUT_RECYCLE_VIEW){
//            val view = inflater.inflate(R.layout.dialog_recycleview ,container,false)
//            CustomRecyclerView(
//                requireContext(),
//                view.findViewById(R.id.input_list),
//                adapterDoubleColumn!!,
//                CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
//            ).getRecyclerView()
//            view
//        } else {
//            super.onCreateView(inflater, container, savedInstanceState)
//        }
//    }
}
