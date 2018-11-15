/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copied from org.gradle.internal.os.OperatingSystem and converted to Kotlin

package org.gradleweaver.plugins.jlink

import java.io.File
import java.util.ArrayList
import java.util.LinkedList
import java.util.regex.Pattern

internal fun withExtension(path: String, extension: String): String {
  return when {
    path.endsWith(extension) -> path
    else -> "${removeExtension(path)}.$extension"
  }
}

internal fun removeExtension(path: String): String {
  val index = path.lastIndexOf('.')
  if (index == -1) {
    return path
  }
  return path.substring(0, index)
}

abstract class OperatingSystem internal constructor() {
  private val toStringValue: String
  val name: String = System.getProperty("os.name")
  val version: String = System.getProperty("os.version")

  open val isWindows = false

  open val isUnix = false

  open val isMacOsX = false

  open val isLinux = false

  abstract val nativePrefix: String

  abstract val executableSuffix: String

  abstract val sharedLibrarySuffix: String

  abstract val staticLibrarySuffix: String

  abstract val linkLibrarySuffix: String

  abstract val familyName: String

  private val path: List<File>
    get() {
      val path = System.getenv(pathVar) ?: return emptyList()
      val entries = ArrayList<File>()
      for (entry in path.split(Pattern.quote(File.pathSeparator).toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
        entries.add(File(entry))
      }
      return entries
    }

  open val pathVar: String = "PATH"

  init {
    toStringValue = name + " " + version + " " + System.getProperty("os.arch")
  }

  override fun toString(): String {
    return toStringValue
  }

  abstract fun getScriptName(scriptPath: String): String

  abstract fun getExecutableName(executablePath: String): String

  abstract fun getSharedLibraryName(libraryName: String): String

  abstract fun getStaticLibraryName(libraryName: String): String

  abstract fun getLinkLibraryName(libraryPath: String): String

  /**
   * Locates the given executable in the system path. Returns null if not found.
   */
  fun findInPath(name: String): File? {
    val exeName = getExecutableName(name)
    if (exeName.contains(File.separator)) {
      val candidate = File(exeName)
      return if (candidate.isFile) {
        candidate
      } else null
    }
    for (dir in path) {
      val candidate = File(dir, exeName)
      if (candidate.isFile) {
        return candidate
      }
    }

    return null
  }

  fun findAllInPath(name: String): List<File> {
    val all = LinkedList<File>()

    for (dir in path) {
      val candidate = File(dir, name)
      if (candidate.isFile) {
        all.add(candidate)
      }
    }

    return all
  }

  internal class Windows : OperatingSystem() {
    override val nativePrefix: String

    override val isWindows = true

    override val familyName = "windows"

    override val executableSuffix = ".exe"

    override val sharedLibrarySuffix = ".dll"

    override val linkLibrarySuffix = ".lib"

    override val staticLibrarySuffix = ".lib"

    override val pathVar = "Path"

    init {
      nativePrefix = resolveNativePrefix()
    }

    override fun getScriptName(scriptPath: String): String = withExtension(scriptPath, ".bat")

    override fun getExecutableName(executablePath: String): String = withExtension(executablePath, ".exe")

    override fun getSharedLibraryName(libraryName: String): String = withExtension(libraryName, ".dll")

    override fun getLinkLibraryName(libraryPath: String): String = withExtension(libraryPath, ".lib")

    override fun getStaticLibraryName(libraryName: String): String = withExtension(libraryName, ".lib")

    private fun resolveNativePrefix(): String {
      var arch = System.getProperty("os.arch")
      if ("i386" == arch) {
        arch = "x86"
      }
      return "win32-$arch"
    }
  }

  internal open class Unix : OperatingSystem() {
    override val nativePrefix: String

    override val familyName = "unknown"

    override val executableSuffix = ""

    override val sharedLibrarySuffix = ".so"

    override val linkLibrarySuffix
      get() = sharedLibrarySuffix

    override val staticLibrarySuffix = ".a"

    override val isUnix = true

    protected open val arch: String
      get() {
        val arch = System.getProperty("os.arch")
        return when(arch) {
          "x86" -> "i386"
          "x86_64" -> "amd64"
          "powerpc" -> "ppc"
          else -> arch
        }
      }

    protected open val osPrefix: String
      get() {
        var osPrefix = name.toLowerCase()
        val space = osPrefix.indexOf(" ")
        if (space != -1) {
          osPrefix = osPrefix.substring(0, space)
        }
        return osPrefix
      }

    init {
      nativePrefix = resolveNativePrefix()
    }

    override fun getScriptName(scriptPath: String) = scriptPath

    override fun getExecutableName(executablePath: String) = executablePath

    override fun getSharedLibraryName(libraryName: String) = getLibraryName(libraryName, sharedLibrarySuffix)

    private fun getLibraryName(libraryName: String, suffix: String): String {
      if (libraryName.endsWith(suffix)) {
        return libraryName
      }
      val pos = libraryName.lastIndexOf('/')
      return when {
        pos >= 0 -> libraryName.substring(0, pos + 1) + "lib" + libraryName.substring(pos + 1) + suffix
        else -> "lib$libraryName$suffix"
      }
    }

    override fun getLinkLibraryName(libraryPath: String) = getSharedLibraryName(libraryPath)

    override fun getStaticLibraryName(libraryName: String) = getLibraryName(libraryName, ".a")

    private fun resolveNativePrefix(): String = "$osPrefix-$arch"
  }

  internal class MacOs : Unix() {
    override val isMacOsX = true

    override val familyName = "os x"

    override val sharedLibrarySuffix = ".dylib"

    override val nativePrefix = "darwin"
  }

  internal class Linux : Unix() {
    override val isLinux = true

    override val familyName = "linux"
  }

  internal class FreeBSD : Unix()

  internal class Solaris : Unix() {
    override val familyName = "solaris"


    override val osPrefix = "sunos"

    override val arch: String
      get() {
        val arch = System.getProperty("os.arch")
        return when (arch) {
          "i386", "x86" -> "x86"
          else -> super.arch
        }
      }
  }

  companion object {
    @JvmField
    val WINDOWS: OperatingSystem = Windows()
    @JvmField
    val MAC_OS: OperatingSystem = MacOs()
    @JvmField
    val SOLARIS: OperatingSystem = Solaris()
    @JvmField
    val LINUX: OperatingSystem = Linux()
    @JvmField
    val FREE_BSD: OperatingSystem = FreeBSD()
    @JvmField
    val UNIX: OperatingSystem = Unix()

    private val currentOs: OperatingSystem by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      forName(System.getProperty("os.name"))
    }

    @JvmStatic
    fun current(): OperatingSystem {
      return currentOs
    }

    @JvmStatic
    fun forName(os: String): OperatingSystem {
      val osName = os.toLowerCase()
      return when {
        osName.contains("windows") -> WINDOWS
        osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx") -> MAC_OS
        osName.contains("sunos") || osName.contains("solaris") -> SOLARIS
        osName.contains("linux") -> LINUX
        osName.contains("freebsd") -> FREE_BSD
        else -> UNIX // Not strictly true
      }
    }
  }
}
