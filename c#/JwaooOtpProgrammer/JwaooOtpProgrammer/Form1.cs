﻿using System;
using System.IO;
using System.Collections.Generic;
using System.Text;
using System.Windows.Forms;
using System.Diagnostics;

namespace JwaooOtpProgrammer {

    public partial class Form1 : Form {

        private static String[] sProgramFiles = {
            "C:\\Program Files (x86)", "C:\\Program Files", "D:\\Program Files (x86)", "D:\\Program Files"
        };

        private static String sFileBdAddrTxt = Path.Combine(Application.StartupPath, "mac.txt");
        private static String sFileHeaderTxt = Path.Combine(Application.StartupPath, "header.txt");
        private static String sFileFirmwareTxt = Path.Combine(Application.StartupPath, "firmware.txt");
        private static String sFileProgrammerBin = Path.Combine(Application.StartupPath, "jtag_programmer.bin");
        private static String[] sOtpCommandArgs = { "-type", "otp", "-chip", "DA14580-01", "-jtag", "123456", "-baudrate", "57600", "-firmware", sFileProgrammerBin };

        private UInt64 mBdAddress;
        private String mFileSmartSnippetsExe;

        delegate void Process_OutputDataReceivedDelegate(object sender, DataReceivedEventArgs e);

        public Form1() {
            InitializeComponent();
            openFileDialogFirmware.InitialDirectory = Application.StartupPath;
            openFileDialogSmartSnippets.InitialDirectory = Application.StartupPath;
            setBdAddress(readBdAddressFile());
        }

        private void setBdAddress(UInt64 addr) {
            mBdAddress = addr;
            textBoxBdAddress.Text = getBdAddressString(addr);
        }

        private bool addBdAddress() {
            UInt64 addr = mBdAddress + 1;

            if (writeBdAddressFile(addr)) {
                setBdAddress(addr);
                return true;
            }

            return false;
        }

        private String getBdAddressString(UInt64 value) {
            StringBuilder builder = new StringBuilder();

            for (int offset = 40; offset >= 0; offset -= 8) {
                builder.Append(valueToChar((int) ((value >> (offset + 4)) & 0x0F)));
                builder.Append(valueToChar((int) ((value >> offset) & 0x0F)));

                if (offset > 0) {
                    builder.Append(':');
                }
            }

            return builder.ToString();
        }

        private UInt64 getBdAddressValue(String text) {
            if (text == null) {
                return 0;
            }

            String[] texts = text.Split(':', '-');
            if (texts.Length != 6) {
                return 0;
            }

            UInt64 value = 0;

            try {
                foreach (String node in texts) {
                    if (node.Length != 2) {
                        return 0;
                    }

                    value = (value << 8) | Convert.ToByte(node, 16);
                }
            } catch {
                return 0;
            }

            return value;
        }

        private byte[] getBdAddressBytes(UInt64 addr) {
            byte[] bytes = new byte[6];

            for (int i = bytes.Length - 1; i >= 0;  i--) {
                bytes[i] = (byte)(addr >> (i * 8));
            }

            return bytes;
        }

        private UInt64 readBdAddressFile() {
            FileStream stream = null;

            try {
                stream = File.OpenRead(sFileBdAddrTxt);

                byte[] buff = new byte[32];
                int length = stream.Read(buff, 0, buff.Length);
                String text = Encoding.ASCII.GetString(buff, 0, length);
                UInt64 addr = getBdAddressValue(text);
                if (addr != 0) {
                    return addr;
                }

                if (text.Length > 0) {
                    MessageBox.Show("MAC地址文件：" + sFileBdAddrTxt + "\r\n格式错误：" + text);
                } else {
                    MessageBox.Show("MAC地址文件为空：" + sFileBdAddrTxt);
                }
            } catch (FileNotFoundException) {
                UInt64 addr = 0x88EA00000000;
                writeBdAddressFile(addr);
                return addr;
            } catch (Exception e) {
                MessageBox.Show("读取MAC地址文件失败：\r\n" + e);
            } finally {
                if (stream != null) {
                    stream.Close();
                }
            }

            return 0;
        }

        private bool writeBdAddressFile(UInt64 addr) {
            String text = getBdAddressString(addr);
            if (text == null) {
                return false;
            }

            byte[] bytes = Encoding.ASCII.GetBytes(text);

            FileStream stream = null;

            try {
                stream = File.OpenWrite(sFileBdAddrTxt);
                stream.Write(bytes, 0, bytes.Length);
                return true;
            } catch (Exception) {
                return false;
            } finally {
                if (stream != null) {
                    stream.Close();
                }
            }
        }

        private String findSmartSnippetsPath() {
            foreach (String path in sProgramFiles) {
                String rootDir = Path.Combine(path, "SmartSnippets");
                if (Directory.Exists(rootDir)) {
                    String binDir = Path.Combine(rootDir, "bin");
                    if (Directory.Exists(binDir)) {
                        String smartSnippetsPath = Path.Combine(binDir, "SmartSnippets.exe");
                        if (File.Exists(smartSnippetsPath)) {
                            return smartSnippetsPath;
                        }
                    }
                }
            }

            return null;
        }

        private String getSmartSnippetsPath() {
            if (mFileSmartSnippetsExe != null && File.Exists(mFileSmartSnippetsExe)) {
                return mFileSmartSnippetsExe;
            }

            mFileSmartSnippetsExe = findSmartSnippetsPath();
            if (mFileSmartSnippetsExe != null) {
                return mFileSmartSnippetsExe;
            }

            if (openFileDialogSmartSnippets.ShowDialog() != DialogResult.OK) {
                return null;
            }

            mFileSmartSnippetsExe = openFileDialogSmartSnippets.FileName;

            return mFileSmartSnippetsExe;
        }

        private ShellCommandRunner doRunCommand(ShellCommandRunner runner) {
            if (runner.execute()) {
                foreach (String line in runner.OutputLines) {
                    textBoxLog.AppendText(line);
                    textBoxLog.AppendText("\r\n");
                }

                textBoxLog.AppendText("============================================================\r\n");

                return runner;
            }

            return null;
        }

        private ShellCommandRunner runSmartSnippetsCommand(params String[] args) {
            ShellCommandRunner runner = new ShellCommandRunner(getSmartSnippetsPath());
            runner.addArguments(args);
            return doRunCommand(runner);
        }

        private ShellCommandRunner runOtpCommand(params String[] args) {
            ShellCommandRunner runner = new ShellCommandRunner(getSmartSnippetsPath());
            runner.addArguments(sOtpCommandArgs);
            runner.addArguments(args);
            return doRunCommand(runner);
        }

        private bool writeOtpData(String offset, String data) {
            ShellCommandRunner runner = runOtpCommand("-cmd", "write_field", "-offset", offset, "-data", data);
            if (runner == null) {
                return false;
            }

            String line = runner.LastOutputLine;
            if (line == null) {
                return false;
            }

            if (line.StartsWith("Failed")) {
                return false;
            }

            return line.StartsWith("Burned");
        }

        private char valueToChar(int value) {
            if (value < 10) {
                return (char)('0' + value);
            } else {
                return (char)('A' - 10 + value);
            }
        }

        private String getBytesHexString(byte[] bytes) {
            StringBuilder builder = new StringBuilder();

            foreach (byte value in bytes) {
                builder.Append(valueToChar(value >> 4));
                builder.Append(valueToChar(value & 0x0F));
            }

            return builder.ToString();
        }

        private bool writeOtpData(String offset, byte[] data) {
            return writeOtpData(offset, getBytesHexString(data));
        }

        private bool writeBdAddress() {
            if (mBdAddress == 0) {
                return false;
            }

            return writeOtpData("0x7FD4", getBdAddressBytes(mBdAddress)) && addBdAddress();
        }

        private bool setOtpBootEnable() {
            return writeOtpData("0x7F00", "1234A5A5A5A51234");
        }

        private bool readOtpHeader(String pathname) {
            ShellCommandRunner runner = runOtpCommand("-cmd", "read_header", "-file", pathname);
            if (runner == null) {
                return false;
            }

            String line = runner.LastOutputLine;
            if (line == null) {
                return false;
            }

            if (line.StartsWith("Failed")) {
                return false;
            }

            return line.StartsWith("Reading is complete");
        }

        private bool readOtpFirmware(String pathname) {
            ShellCommandRunner runner = runOtpCommand("-cmd", "read_custom_code", "-file", pathname);
            if (runner == null) {
                return false;
            }

            String line = runner.LastOutputLine;
            if (line == null) {
                return false;
            }

            if (line.StartsWith("OTP memory reading has failed")) {
                return false;
            }

            return line.StartsWith("OTP memory reading has finished");
        }

        private bool writeOtpFirmware(String pathname) {
            ShellCommandRunner runner = runOtpCommand("-cmd", "write_custom_code", "-y", "-file", pathname);
            if (runner == null) {
                return false;
            }

            String line = runner.LastOutputLine;
            if (line == null) {
                return false;
            }

            if (line.StartsWith("OTP Memory burning failed")) {
                return false;
            }

            return line.StartsWith("OTP Memory burning completed successfully");
        }

        private void buttonFirmware_Click(object sender, EventArgs e) {
            if (openFileDialogFirmware.ShowDialog() == DialogResult.OK) {
                textBoxFirmware.Text = openFileDialogFirmware.FileName;
            }
        }

        private void buttonConnect_Click(object sender, EventArgs e) {
            if (readOtpHeader(sFileHeaderTxt)) {
                MessageBox.Show("连接成功");
            } else {
                MessageBox.Show("连接失败");
            }
        }

        private void buttonBurn_Click(object sender, EventArgs e) {
            String pathname = textBoxFirmware.Text;
            if (pathname == null || pathname.Length == 0) {
                MessageBox.Show("请选择固件文件");
                return;
            }

            if (!File.Exists(pathname)) {
                MessageBox.Show("固件文件不存在：" + pathname);
                return;
            }

            if (!readOtpFirmware(sFileFirmwareTxt)) {
                MessageBox.Show("读取固件失败");
                return;
            }

            if (!writeOtpFirmware(pathname)) {
                MessageBox.Show("写固件失败: " + pathname);
                return;
            }

            if (!setOtpBootEnable()) {
                MessageBox.Show("设置从OTP启动失败");
                return;
            }

            if (!writeBdAddress()) {
                MessageBox.Show("写MAC地址失败");
                return;
            }

            MessageBox.Show("恭喜，烧录成功");
        }

        private void buttonClearLog_Click(object sender, EventArgs e) {
            textBoxLog.Clear();
        }
    }

    public class ShellCommandRunner {

        private String mCommand;
        private List<String> mErrLines = new List<string>();
        private List<String> mOutLines = new List<string>();
        private List<String> mArgumants = new List<string>();

        public ShellCommandRunner(String command) {
            mCommand = command;
        }

        public void setArguments(List<String> args) {
            if (args == null) {
                mArgumants.Clear();
            } else {
                mArgumants = args;
            }
        }

        public void setArguments(params String[] args) {
            mArgumants = new List<string>(args.Length);
            addArguments(args);
        }

        public void addArgument(String arg) {
            mArgumants.Add(arg);
        }

        public void addArguments(params String[] args) {
            foreach (String arg in args) {
                mArgumants.Add(arg);
            }
        }

        public void addArguments(List<String> args) {
            foreach (String arg in args) {
                mArgumants.Add(arg);
            }
        }

        private String linesToString(List<String> lines) {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < lines.Count; i++) {
                builder.Append(i + ". " + lines[i] + "\r\n");
            }

            return builder.ToString();
        }

        public bool execute() {
            if (mCommand == null) {
                return false;
            }

            Process process = new Process();

            if (mArgumants != null && mArgumants.Count > 0) {
                bool needSpace = false;
                StringBuilder builder = new StringBuilder();

                foreach (String arg in mArgumants) {
                    if (needSpace) {
                        builder.Append(" \"");
                    } else {
                        needSpace = true;
                        builder.Append('"');
                    }

                    builder.Append(arg);
                    builder.Append('"');
                }

                process.StartInfo.Arguments = builder.ToString();
            }

            process.StartInfo.FileName = mCommand;
            process.StartInfo.UseShellExecute = false;
            process.StartInfo.CreateNoWindow = true;
            process.StartInfo.RedirectStandardOutput = true;
            process.StartInfo.RedirectStandardError = true;
            process.OutputDataReceived += Process_OutputDataReceived;
            process.ErrorDataReceived += Process_ErrorDataReceived;

#if true
            MessageBox.Show("Arguments = " + process.StartInfo.Arguments);
#endif

            mOutLines.Clear();
            mErrLines.Clear();

            try {
                if (process.Start()) {
                    process.BeginOutputReadLine();
                    process.BeginErrorReadLine();
                    process.WaitForExit();

                    if (process.ExitCode != 0) {
                        return false;
                    }

#if false
                    if (mOutLines.Count > 0) {
                        MessageBox.Show(linesToString(mOutLines));
                    }
#endif

                    return true;
                }
            } catch (Exception e) {
                MessageBox.Show("Error: " + e);
            }

            return false;
        }

        public List<String> OutputLines {
            get {
                return mOutLines;
            }
        }

        public int OutputLineCount {
            get {
                return mOutLines.Count;
            }
        }

        public String LastOutputLine {
            get {
                int count = mOutLines.Count;
                if (count > 0) {
                    return mOutLines[count - 1];
                }

                return null;
            }
        }

        public List<String> ErrorLines {
            get {
                return mErrLines;
            }
        }

        public String getOutputLine(int index) {
            return mOutLines[index];
        }

        public String getErrorLine(int index) {
            return mErrLines[index];
        }

        private void Process_ErrorDataReceived(object sender, DataReceivedEventArgs e) {
            mErrLines.Add(e.Data);
        }

        private void Process_OutputDataReceived(object sender, DataReceivedEventArgs e) {
            if (e.Data != null) {
                mOutLines.Add(e.Data);
            }
        }
    }
}
