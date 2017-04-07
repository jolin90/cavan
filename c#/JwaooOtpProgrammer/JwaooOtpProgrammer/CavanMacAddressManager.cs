﻿using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace JwaooOtpProgrammer {
    public partial class CavanMacAddressManager : Form {

        private CavanMacAddressButton mAddressCurrent;
        private CavanMacAddressRange mAddressRange;
        private object mSenderLocked;

        public CavanMacAddressManager(CavanMacAddress address, UInt32 count) {
            InitializeComponent();

            mAddressRange = new CavanMacAddressRange(address, count);

            Rectangle bounds = panelAddresses.Bounds;
            bounds.X = 0;
            bounds.Y = 0;

            CavanMacAddressItem item = new CavanMacAddressItem(this, bounds, address, count);
            addMacAddressItem(0, item);

            AddressStart = address;
            AddressCount = count;

            if (count > 0) {
                address.add(count);
                AddressNext = address;

                address.decrease();
                AddressEnd = address;
            } else {
                AddressEnd = address;
                AddressNext = address;
            }
        }

        public ContextMenuStrip getContextMenuStripAlloc() {
            return contextMenuStripAlloc;
        }

        public ContextMenuStrip getContextMenuStripFree() {
            return contextMenuStripFree;
        }

        public void setCurrentMacAddressButton(CavanMacAddressButton button) {
            if (mAddressCurrent != null) {
                mAddressCurrent.setHighLight(false);
            }

            mAddressCurrent = button;
            mAddressCurrent.setHighLight(true);

            updateContextMenuStrip();
        }

        public void updateContextMenuStrip() {
            if (mAddressCurrent == null) {
                listViewAddresses.ContextMenuStrip = null;
            } else if (mAddressCurrent.AddressUsed) {
                listViewAddresses.ContextMenuStrip = contextMenuStripFree;
            } else {
                listViewAddresses.ContextMenuStrip = contextMenuStripAlloc;
            }
        }

        public void addMacAddressItem(int index, CavanMacAddressItem item) {
            CavanMacAddressButton button = item.AddressButton;
            button.GotFocus += buttonMacAddress_GotFocus;
            button.MouseEnter += buttonMacAddress_MouseEnter;
            panelAddresses.Controls.Add(button);

            listViewAddresses.Items.Insert(index, item);
        }

        public void joinMacAddressItems(ListView.ListViewItemCollection items, int index, int count) {
            int width = 0;
            UInt32 addresses = 0;

            while (true) {
                CavanMacAddressItem item = (CavanMacAddressItem)listViewAddresses.Items[index--];
                CavanMacAddressButton button = item.AddressButton;

                if (count-- > 1) {
                    width += button.Width + 1;
                    addresses += item.AddressCount;

                    panelAddresses.Controls.Remove(button);
                    listViewAddresses.Items.Remove(item);
                } else {
                    button.Width += width;
                    item.AddressCount += addresses;
                    break;
                }
            }
        }

        public void joinMacAddressItems() {
            ListView.ListViewItemCollection items = listViewAddresses.Items;
            int count = 0;

            foreach (CavanMacAddressItem item in items) {
                if (item.AddressUsed) {
                    if (count > 1) {
                        joinMacAddressItems(items, item.Index - 1, count);
                    }

                    count = 0;
                } else {
                    count++;
                }
            }

            if (count > 1) {
                joinMacAddressItems(items, items.Count - 1, count);
            }
        }

        private void buttonMacAddress_MouseEnter(object sender, EventArgs e) {
            CavanMacAddressButton button = (CavanMacAddressButton)sender;
            button.Focus();
        }

        private void buttonMacAddress_GotFocus(object sender, EventArgs e) {
            setCurrentMacAddressButton((CavanMacAddressButton)sender);
        }

        private void listViewAddresses_SelectedIndexChanged(object sender, EventArgs e) {
            ListView.SelectedListViewItemCollection items = listViewAddresses.SelectedItems;
            if (items != null && items.Count > 0) {
                CavanMacAddressItem item = (CavanMacAddressItem)items[0];
                setCurrentMacAddressButton(item.AddressButton);
            }
        }

        private void freeMacAddress() {
            if (mAddressCurrent != null) {
                mAddressCurrent.free();
            }
        }

        private void allocMacAddress() {
            if (mAddressCurrent != null) {
                mAddressCurrent.alloc(100);
            }
        }

        public void setAddressStart(CavanMacAddress address) {
            if (mAddressRange.AddressStart.compareTo(address) == 0) {
                return;
            }

            mAddressRange.AddressStart = address;

            foreach (CavanMacAddressItem item in listViewAddresses.Items) {
                item.AddressStart = address;
                address = item.AddressNext;
            }
        }

        public void setAddressCount(UInt32 count) {
            if (mAddressRange.AddressCount == count) {
                return;
            }

            mAddressRange.AddressCount = count;
        }

        public CavanMacAddress AddressStart {
            get {
                return new CavanMacAddress().fromString(textBoxAddressStart.Text);
            }

            set {
                textBoxAddressStart.Text = value.ToString();
            }
        }

        public UInt32 AddressCount {
            get {
                try {
                    return Convert.ToUInt32(textBoxAddressCount.Text);
                } catch {
                    return 0;
                }
            }

            set {
                textBoxAddressCount.Text = Convert.ToString(value);
                setAddressCount(value);
            }
        }

        public CavanMacAddress AddressEnd {
            get {
                return new CavanMacAddress().fromString(textBoxAddressEnd.Text);
            }

            set {
                textBoxAddressEnd.Text = value.ToString();
            }
        }

        public CavanMacAddress AddressNext {
            get {
                return new CavanMacAddress().fromString(textBoxAddressNext.Text);
            }

            set {
                textBoxAddressNext.Text = value.ToString();
            }
        }

        private void textBoxAddressStart_TextChanged(object sender, EventArgs e) {
            if (acquireEventLock(sender)) {
                UInt32 count = AddressCount;
                CavanMacAddress address = AddressStart;

                setAddressStart(new CavanMacAddress(address));

                if (count > 0) {
                    address.add(count);
                    AddressNext = address;

                    address.decrease();
                    AddressEnd = address;
                } else {
                    AddressNext = address;
                    AddressEnd = address;
                }
            }
        }

        private void textBoxAddressCount_TextChanged(object sender, EventArgs e) {
            textBoxAddressStart_TextChanged(sender, e);
        }

        private void textBoxAddressEnd_TextChanged(object sender, EventArgs e) {
            if (acquireEventLock(sender)) {
                CavanMacAddress address = AddressEnd;

                address.increase();
                AddressNext = address;

                if (address.sub(AddressStart) == 0) {
                    AddressCount = (UInt32)address.toLong();
                } else {
                    AddressCount = 0;
                }
            }
        }

        private void textBoxAddressNext_TextChanged(object sender, EventArgs e) {
            if (acquireEventLock(sender)) {
                CavanMacAddress address = AddressNext;
                CavanMacAddress end = new CavanMacAddress(address);

                end.decrease();
                AddressEnd = end;

                if (address.sub(AddressStart) == 0) {
                    AddressCount = (UInt32)address.toLong();
                } else {
                    AddressCount = 0;
                }
            }
        }

        private void textBoxAddressStart_Leave(object sender, EventArgs e) {
            AddressStart = AddressStart;
        }

        private void textBoxAddressEnd_Leave(object sender, EventArgs e) {
            AddressEnd = AddressEnd;
        }

        private void textBoxAddressCount_Leave(object sender, EventArgs e) {
            AddressCount = AddressCount;

        }

        private void textBoxAddressNext_Leave(object sender, EventArgs e) {
            AddressNext = AddressNext;
        }

        private void toolStripMenuItemFree_Click(object sender, EventArgs e) {
            freeMacAddress();
        }

        private void toolStripMenuItemAlloc_Click(object sender, EventArgs e) {
            allocMacAddress();
        }

        public bool acquireEventLock(object sender) {
            if (mSenderLocked != sender) {
                if (mSenderLocked != null) {
                    return false;
                }

                mSenderLocked = sender;
            }

            timerEventLock.Stop();
            timerEventLock.Start();

            return true;
        }

        private void timerUpdateLock_Tick(object sender, EventArgs e) {
            timerEventLock.Stop();
            mSenderLocked = null;
        }
    }

    public class CavanMacAddressButton : Button {

        private CavanMacAddressItem mAddressItem;

        public CavanMacAddressButton(CavanMacAddressItem item, Rectangle bounds) {
            mAddressItem = item;
            Bounds = bounds;

            FlatStyle = FlatStyle.Flat;
            FlatAppearance.BorderColor = Color.Black;
        }

        public CavanMacAddressItem AddressItem {
            get {
                return mAddressItem;
            }
        }

        public CavanMacAddressManager AddressManager {
            get {
                return mAddressItem.AddressManager;
            }
        }

        public UInt32 AddressCount {
            get {
                return mAddressItem.AddressCount;
            }

            set {
                mAddressItem.AddressCount = value;
            }
        }

        public bool AddressUsed {
            get {
                return mAddressItem.AddressUsed;
            }

            set {
                mAddressItem.AddressUsed = value;
            }
        }

        public CavanMacAddress AddressEnd {
            get {
                return mAddressItem.AddressEnd;
            }
        }

        public CavanMacAddress AddressNext {
            get {
                return mAddressItem.AddressNext;
            }
        }

        public void updateUI() {
            if (AddressUsed) {
                ContextMenuStrip = AddressManager.getContextMenuStripFree();
                Text = "已分配(" + AddressCount + ")";
            } else {
                ContextMenuStrip = AddressManager.getContextMenuStripAlloc();
                Text = "空闲(" + AddressCount + ")";
            }
        }

        public void setHighLight(bool enable) {
            if (enable) {
                BackColor = Color.Blue;
                ForeColor = Color.White;
                FlatAppearance.BorderColor = Color.Red;
            } else {
                BackColor = Color.White;
                ForeColor = Color.Black;
                FlatAppearance.BorderColor = Color.Black;
            }

            mAddressItem.setHighLight(enable);
        }

        public bool alloc(UInt32 count) {
            return mAddressItem.alloc(count);
        }

        public void free() {
            mAddressItem.free();
        }
    }

    public class CavanMacAddressItem : ListViewItem {

        private bool mAddressUsed;
        private CavanMacAddressRange mAddressRange;

        private CavanMacAddressManager mAddressManager;
        private CavanMacAddressButton mAddressButton;

        public CavanMacAddressItem(CavanMacAddressManager manager, Rectangle bounds, CavanMacAddress address, UInt32 count) {
            mAddressManager = manager;
            mAddressUsed = false;

            mAddressRange = new CavanMacAddressRange(address, count);
            mAddressButton = new CavanMacAddressButton(this, bounds);

            SubItems.Add("-");
            SubItems.Add("-");
            SubItems.Add("-");
            updateUI();
        }

        public CavanMacAddress AddressStart {
            get {
                return mAddressRange.AddressStart;
            }

            set {
                mAddressRange.AddressStart = value;
                updateUI();
            }
        }

        public UInt32 AddressCount {
            get {
                return mAddressRange.AddressCount;
            }

            set {
                mAddressRange.AddressCount = value;
                updateUI();
            }
        }

        public bool AddressUsed {
            get {
                return mAddressUsed;
            }

            set {
                mAddressUsed = value;
                updateUI();
            }
        }

        public CavanMacAddress AddressEnd {
            get {
                return mAddressRange.AddressEnd;
            }
        }

        public CavanMacAddress AddressNext {
            get {
                return mAddressRange.AddressNext;
            }
        }

        public CavanMacAddressManager AddressManager {
            get {
                return mAddressManager;
            }
        }

        public CavanMacAddressButton AddressButton {
            get {
                return mAddressButton;
            }
        }

        public void updateUI() {
            Text = AddressStart.ToString();
            SubItems[1].Text = AddressEnd.ToString();
            SubItems[2].Text = Convert.ToString(AddressCount);

            if (mAddressUsed) {
                SubItems[3].Text = "已分配";
            } else {
                SubItems[3].Text = "空闲";
            }

            mAddressButton.updateUI();
        }

        public bool alloc(UInt32 count) {
            if (mAddressUsed || count == 0) {
                return false;
            }

            UInt32 max = mAddressRange.AddressCount;
            if (count > max) {
                return false;
            }

            if (count < max) {
                Rectangle bounds = mAddressButton.Bounds;
                UInt32 remain = max - count;
                int widthNew = (int)(remain * bounds.Width / max);
                int widthOld = bounds.Width - widthNew;

                bounds.Width = widthOld - 1;
                mAddressButton.Bounds = bounds;
                mAddressRange.AddressCount = count;

                bounds.X += widthOld;
                bounds.Width = widthNew;

                CavanMacAddressItem item = new CavanMacAddressItem(mAddressManager, bounds, AddressNext, remain);
                mAddressManager.addMacAddressItem(Index + 1, item);
            }

            AddressUsed = true;
            mAddressManager.updateContextMenuStrip();

            return true;
        }

        public void free() {
            AddressUsed = false;
            mAddressManager.joinMacAddressItems();
            mAddressManager.updateContextMenuStrip();
        }

        public void setHighLight(bool enable) {
            if (enable) {
                BackColor = Color.Blue;
                ForeColor = Color.White;
            } else {
                BackColor = Color.White;
                ForeColor = Color.Black;
            }
        }
    }
}
