#include <jni.h>
#include <string>
#include <thread>
#include <atomic>
#include <chrono>
#include <fcntl.h>
#include <unistd.h>
#include <linux/input.h>
#include <poll.h>
#include <android/log.h>

#define LOG_TAG "MouseLock"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Constants
constexpr int TARGET_X = 540;
constexpr int TARGET_Y = 1170;
constexpr int DELTA_THRESHOLD = 10;
constexpr int COOLDOWN_MS = 8; // 125Hz
constexpr const char* EVENT_DEVICE = "/dev/input/event11";

// Global state
std::atomic<bool> g_running{false};
std::atomic<bool> g_locked{false};
std::thread g_eventThread;
FILE* g_shellPipe = nullptr;

// High-performance event loop
void eventLoop() {
    int fd = open(EVENT_DEVICE, O_RDONLY | O_NONBLOCK);
    if (fd < 0) {
        LOGE("Failed to open %s", EVENT_DEVICE);
        return;
    }

    struct pollfd pfd = {fd, POLLIN, 0};
    struct input_event ev;
    int accumulated_x = 0;
    int accumulated_y = 0;
    auto last_warp = std::chrono::steady_clock::now();

    LOGI("Event loop started");

    while (g_running.load(std::memory_order_relaxed)) {
        int ret = poll(&pfd, 1, 100); // 100ms timeout
        
        if (ret > 0 && (pfd.revents & POLLIN)) {
            ssize_t bytes = read(fd, &ev, sizeof(ev));
            
            if (bytes == sizeof(ev) && g_locked.load(std::memory_order_relaxed)) {
                // Track relative movement
                if (ev.type == EV_REL) {
                    if (ev.code == REL_X) {
                        accumulated_x += ev.value;
                    } else if (ev.code == REL_Y) {
                        accumulated_y += ev.value;
                    }
                }
                
                // Check if we should warp
                if (ev.type == EV_SYN && ev.code == SYN_REPORT) {
                    auto now = std::chrono::steady_clock::now();
                    auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
                        now - last_warp
                    ).count();

                    if ((abs(accumulated_x) > DELTA_THRESHOLD || 
                         abs(accumulated_y) > DELTA_THRESHOLD) &&
                        elapsed >= COOLDOWN_MS) {
                        
                        // Execute warp command
                        if (g_shellPipe) {
                            fprintf(g_shellPipe, "input mouse tap %d %d\n", TARGET_X, TARGET_Y);
                            fflush(g_shellPipe);
                        }
                        
                        accumulated_x = 0;
                        accumulated_y = 0;
                        last_warp = now;
                    }
                }

                // Handle M4/M5 buttons (optional feature)
                if (ev.type == EV_KEY) {
                    if (ev.code == 0x113 || ev.code == 0x114) {
                        LOGI("Side button detected: 0x%x", ev.code);
                    }
                }
            } else {
                // Reset accumulator when unlocked
                accumulated_x = 0;
                accumulated_y = 0;
            }
        }
    }

    close(fd);
    LOGI("Event loop stopped");
}

extern "C" JNIEXPORT void JNICALL
Java_com_mouselock_app_MouseService_nativeStartLock(
        JNIEnv* env,
        jobject /* this */,
        jstring shPath) {
    
    if (g_running.load()) {
        LOGI("Lock already running");
        return;
    }

    const char* path = env->GetStringUTFChars(shPath, nullptr);
    
    // Open persistent shell pipe
    std::string command = std::string(path) + " 2>&1";
    g_shellPipe = popen(command.c_str(), "w");
    
    env->ReleaseStringUTFChars(shPath, path);

    if (!g_shellPipe) {
        LOGE("Failed to open shell pipe");
        return;
    }

    // Make pipe non-blocking
    int fd = fileno(g_shellPipe);
    fcntl(fd, F_SETFL, O_NONBLOCK);

    g_running.store(true);
    g_eventThread = std::thread(eventLoop);
    
    LOGI("Native lock started");
}

extern "C" JNIEXPORT void JNICALL
Java_com_mouselock_app_MouseService_nativeStopLock(
        JNIEnv* env,
        jobject /* this */) {
    
    g_running.store(false);
    
    if (g_eventThread.joinable()) {
        g_eventThread.join();
    }

    if (g_shellPipe) {
        pclose(g_shellPipe);
        g_shellPipe = nullptr;
    }

    LOGI("Native lock stopped");
}

extern "C" JNIEXPORT void JNICALL
Java_com_mouselock_app_MouseService_nativeSetLocked(
        JNIEnv* env,
        jobject /* this */,
        jboolean locked) {
    
    g_locked.store(locked);
    LOGI("Lock state: %s", locked ? "LOCKED" : "UNLOCKED");
}