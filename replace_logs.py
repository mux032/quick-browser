#!/usr/bin/env python3

import os
import re

# Directory to search
directory = "app/src/main/java"

# Function to replace Log calls with Logger calls
def replace_log_calls(file_path):
    with open(file_path, 'r') as file:
        content = file.read()
    
    # Replace Log.d(TAG, "message") with Logger.d(TAG, "message")
    content = re.sub(r'Log\.d\(([^,]+),\s*("[^"]*")\)', r'Logger.d(\1, \2)', content)
    
    # Replace Log.e(TAG, "message") with Logger.e(TAG, "message")
    content = re.sub(r'Log\.e\(([^,]+),\s*("[^"]*")\)', r'Logger.e(\1, \2)', content)
    
    # Replace Log.w(TAG, "message") with Logger.w(TAG, "message")
    content = re.sub(r'Log\.w\(([^,]+),\s*("[^"]*")\)', r'Logger.w(\1, \2)', content)
    
    # Replace Log.i(TAG, "message") with Logger.i(TAG, "message")
    content = re.sub(r'Log\.i\(([^,]+),\s*("[^"]*")\)', r'Logger.i(\1, \2)', content)
    
    # Replace Log.v(TAG, "message") with Logger.v(TAG, "message")
    content = re.sub(r'Log\.v\(([^,]+),\s*("[^"]*")\)', r'Logger.v(\1, \2)', content)
    
    # Replace Log.e(TAG, "message", throwable) with Logger.e(TAG, "message", throwable)
    content = re.sub(r'Log\.e\(([^,]+),\s*("[^"]*"),\s*([^\)]+)\)', r'Logger.e(\1, \2, \3)', content)
    
    # Replace Log.w(TAG, "message", throwable) with Logger.w(TAG, "message", throwable)
    content = re.sub(r'Log\.w\(([^,]+),\s*("[^"]*"),\s*([^\)]+)\)', r'Logger.w(\1, \2, \3)', content)
    
    # Replace import android.util.Log with import com.quick.browser.util.Logger
    content = re.sub(r'import android\.util\.Log', 'import com.quick.browser.util.Logger', content)
    
    with open(file_path, 'w') as file:
        file.write(content)

# Walk through directory and process .kt files
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith(".kt"):
            file_path = os.path.join(root, file)
            replace_log_calls(file_path)
            print(f"Processed {file_path}")

print("Finished processing all Kotlin files")