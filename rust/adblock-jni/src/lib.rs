use std::panic::{catch_unwind, AssertUnwindSafe};
use std::ptr::{self, NonNull};

use adblock::lists::ParseOptions;
use adblock::request::Request;
use adblock::Engine;
use jni::objects::{JByteArray, JClass, JObject, JObjectArray, JString};
use jni::sys::{jboolean, jbyteArray, jlong, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use serde::Serialize;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct CosmeticResourcesDto {
    hide_selectors: Vec<String>,
    injected_script: String,
    generichide: bool,
    exceptions: Vec<String>,
}

fn jstring_to_string(env: &mut JNIEnv, input: &JString) -> Option<String> {
    if input.is_null() {
        return None;
    }
    env.get_string(input).ok().map(|s| s.into())
}

fn jobject_array_to_vec(env: &mut JNIEnv, array: &JObjectArray) -> Vec<String> {
    if array.is_null() {
        return Vec::new();
    }
    let len = match env.get_array_length(array) {
        Ok(l) if l > 0 => l,
        _ => return Vec::new(),
    };
    let mut out = Vec::with_capacity(len as usize);
    for i in 0..len {
        if let Ok(obj) = env.get_object_array_element(array, i) {
            if !obj.is_null() {
                let jstr = JString::from(obj);
                if let Some(s) = jstring_to_string(env, &jstr) {
                    out.push(s);
                }
                let _ = env.delete_local_ref(jstr);
            }
        }
    }
    out
}

fn strings_to_jobject_array<'local>(
    env: &mut JNIEnv<'local>,
    strings: &[String],
) -> JObjectArray<'local> {
    let Ok(str_cls) = env.find_class("java/lang/String") else {
        return JObjectArray::from(JObject::null());
    };
    let Ok(out_arr) = env.new_object_array(strings.len() as i32, &str_cls, JObject::null()) else {
        let _ = env.delete_local_ref(str_cls);
        return JObjectArray::from(JObject::null());
    };
    let _ = env.delete_local_ref(str_cls);
    for (idx, s) in strings.iter().enumerate() {
        if let Ok(jstr) = env.new_string(s) {
            let _ = env.set_object_array_element(&out_arr, idx as i32, &jstr);
            let _ = env.delete_local_ref(jstr);
        }
    }
    out_arr
}

fn validate_handle(handle: jlong) -> Option<NonNull<Engine>> {
    NonNull::new(handle as *mut Engine)
}

/// Converts a raw JNI `jlong` handle into a shared reference to [`Engine`].
///
/// # Safety & Locking Invariant
/// - `handle` must either be `0` or a valid pointer previously returned by
///   `Box::into_raw(Box::new(engine))` that has not yet been passed to `nativeDestroy`.
/// - Concurrent access relies on Kotlin's `ReentrantReadWriteLock`: callers of
///   read/query methods (`nativeSerialize`, `nativeCheckNetwork`,
///   `nativeCosmeticResourcesJson`, `nativeHiddenClassIdSelectors`) must hold
///   the read lock (or write lock), while `nativeDestroy` must hold the write lock
///   so no shared `&Engine` reference exists when the `Engine` is dropped.
unsafe fn engine_from_handle<'a>(handle: jlong) -> Option<&'a Engine> {
    validate_handle(handle).map(|ptr| ptr.as_ref())
}

#[no_mangle]
pub extern "system" fn Java_com_znarfm_siphon_core_adblock_NativeAdblockJni_nativeCreateFromRules(
    mut env: JNIEnv,
    _class: JClass,
    rules: JString,
) -> jlong {
    let result = catch_unwind(AssertUnwindSafe(|| -> jlong {
        let Some(rules_str) = jstring_to_string(&mut env, &rules) else {
            return 0;
        };
        let mut filter_set = adblock::lists::FilterSet::new(false);
        filter_set.add_filter_list(rules_str, ParseOptions::default());
        let engine = Engine::new_with_filter_set(filter_set);
        Box::into_raw(Box::new(engine)) as jlong
    }));
    result.unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_com_znarfm_siphon_core_adblock_NativeAdblockJni_nativeCreateFromDat(
    env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jlong {
    let result = catch_unwind(AssertUnwindSafe(|| -> jlong {
        if data.is_null() {
            return 0;
        }
        let Ok(bytes) = env.convert_byte_array(&data) else {
            return 0;
        };
        let mut engine = Engine::default();
        if engine.deserialize(&bytes).is_err() {
            return 0;
        }
        Box::into_raw(Box::new(engine)) as jlong
    }));
    result.unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_com_znarfm_siphon_core_adblock_NativeAdblockJni_nativeSerialize(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jbyteArray {
    let result = catch_unwind(AssertUnwindSafe(|| -> jbyteArray {
        let Some(engine) = (unsafe { engine_from_handle(handle) }) else {
            return ptr::null_mut();
        };
        let bytes = engine.serialize();
        match env.byte_array_from_slice(&bytes) {
            Ok(arr) => arr.into_raw(),
            Err(_) => ptr::null_mut(),
        }
    }));
    result.unwrap_or(ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_znarfm_siphon_core_adblock_NativeAdblockJni_nativeCheckNetwork(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    url: JString,
    source_url: JString,
    request_type: JString,
) -> jboolean {
    let result = catch_unwind(AssertUnwindSafe(|| -> jboolean {
        let Some(engine) = (unsafe { engine_from_handle(handle) }) else {
            return JNI_FALSE;
        };
        let Some(url_str) = jstring_to_string(&mut env, &url) else {
            return JNI_FALSE;
        };
        let source_str = jstring_to_string(&mut env, &source_url).unwrap_or_default();
        let req_type_str = jstring_to_string(&mut env, &request_type).unwrap_or_else(|| "other".to_string());

        let Ok(request) = Request::new(&url_str, &source_str, &req_type_str, "GET") else {
            return JNI_FALSE;
        };
        let blocker_result = engine.check_network_request(&request);
        if blocker_result.should_block() {
            JNI_TRUE
        } else {
            JNI_FALSE
        }
    }));
    result.unwrap_or(JNI_FALSE)
}

#[no_mangle]
pub extern "system" fn Java_com_znarfm_siphon_core_adblock_NativeAdblockJni_nativeCosmeticResourcesJson(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    url: JString,
) -> jstring {
    let result = catch_unwind(AssertUnwindSafe(|| -> jstring {
        let Some(engine) = (unsafe { engine_from_handle(handle) }) else {
            return ptr::null_mut();
        };
        let Some(url_str) = jstring_to_string(&mut env, &url) else {
            return ptr::null_mut();
        };
        let resources = engine.url_cosmetic_resources(&url_str);
        let dto = CosmeticResourcesDto {
            hide_selectors: resources.hide_selectors.into_iter().collect(),
            injected_script: resources.injected_script,
            generichide: resources.generichide,
            exceptions: resources.exceptions.into_iter().collect(),
        };
        let Ok(json) = serde_json::to_string(&dto) else {
            return ptr::null_mut();
        };
        match env.new_string(json) {
            Ok(s) => s.into_raw(),
            Err(_) => ptr::null_mut(),
        }
    }));
    result.unwrap_or(ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_znarfm_siphon_core_adblock_NativeAdblockJni_nativeHiddenClassIdSelectors<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: jlong,
    classes: JObjectArray<'local>,
    ids: JObjectArray<'local>,
    exceptions: JObjectArray<'local>,
) -> JObjectArray<'local> {
    let result = catch_unwind(AssertUnwindSafe(|| -> JObjectArray<'local> {
        let Some(engine) = (unsafe { engine_from_handle(handle) }) else {
            return strings_to_jobject_array(&mut env, &[]);
        };
        let class_vec = jobject_array_to_vec(&mut env, &classes);
        let id_vec = jobject_array_to_vec(&mut env, &ids);
        let exception_set: std::collections::HashSet<String> =
            jobject_array_to_vec(&mut env, &exceptions).into_iter().collect();

        let selectors = engine.hidden_class_id_selectors(&class_vec, &id_vec, &exception_set);
        strings_to_jobject_array(&mut env, &selectors)
    }));
    result.unwrap_or_else(|_| JObjectArray::from(JObject::null()))
}

#[no_mangle]
pub extern "system" fn Java_com_znarfm_siphon_core_adblock_NativeAdblockJni_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        if let Some(ptr) = validate_handle(handle) {
            unsafe {
                drop(Box::from_raw(ptr.as_ptr()));
            }
        }
    }));
}
