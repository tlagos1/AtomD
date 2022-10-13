package com.sorbonne.atom_d

inline fun <T> T?.guard(block: T?.() -> Unit): T {
    if (this == null) block(); return this!!
}