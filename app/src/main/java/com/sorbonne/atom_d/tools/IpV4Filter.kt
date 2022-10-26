package com.sorbonne.atom_d.tools

import android.text.InputFilter
import android.text.Spanned


class IpV4Filter: InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int,
                        dest: Spanned , dstart: Int , dend: Int): CharSequence? {
        if (end > start) {
            val destTxt = dest.toString()
            val resultingTxt = (destTxt.substring(0, dstart)
                    + source.subSequence(start, end)
                    ) + destTxt.substring(dend)
            if (!resultingTxt
                    .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?".toRegex())
            ) {
                return ""
            } else {
                val splits = resultingTxt.split(".").toTypedArray()
                for (i in splits.indices) {
                    if(splits[i].isNotBlank()){
                        if (Integer.valueOf(splits[i]) > 255) {
                            return ""
                        }
                    }
                }
            }
        }
        return null
    }
}