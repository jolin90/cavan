#include <cavan.h>
#include <cavan/ecc.h>

// Fuang.Cao <cavan.cfa@gmail.com> 2011-12-23 16:39:57

static const u8 cavan_ecc_table[] = {
	0x00, 0x55, 0x56, 0x03, 0x59, 0x0c, 0x0f, 0x5a, 0x5a, 0x0f,
	0x0c, 0x59, 0x03, 0x56, 0x55, 0x00, 0x65, 0x30, 0x33, 0x66,
	0x3c, 0x69, 0x6a, 0x3f, 0x3f, 0x6a, 0x69, 0x3c, 0x66, 0x33,
	0x30, 0x65, 0x66, 0x33, 0x30, 0x65, 0x3f, 0x6a, 0x69, 0x3c,
	0x3c, 0x69, 0x6a, 0x3f, 0x65, 0x30, 0x33, 0x66, 0x03, 0x56,
	0x55, 0x00, 0x5a, 0x0f, 0x0c, 0x59, 0x59, 0x0c, 0x0f, 0x5a,
	0x00, 0x55, 0x56, 0x03, 0x69, 0x3c, 0x3f, 0x6a, 0x30, 0x65,
	0x66, 0x33, 0x33, 0x66, 0x65, 0x30, 0x6a, 0x3f, 0x3c, 0x69,
	0x0c, 0x59, 0x5a, 0x0f, 0x55, 0x00, 0x03, 0x56, 0x56, 0x03,
	0x00, 0x55, 0x0f, 0x5a, 0x59, 0x0c, 0x0f, 0x5a, 0x59, 0x0c,
	0x56, 0x03, 0x00, 0x55, 0x55, 0x00, 0x03, 0x56, 0x0c, 0x59,
	0x5a, 0x0f, 0x6a, 0x3f, 0x3c, 0x69, 0x33, 0x66, 0x65, 0x30,
	0x30, 0x65, 0x66, 0x33, 0x69, 0x3c, 0x3f, 0x6a, 0x6a, 0x3f,
	0x3c, 0x69, 0x33, 0x66, 0x65, 0x30, 0x30, 0x65, 0x66, 0x33,
	0x69, 0x3c, 0x3f, 0x6a, 0x0f, 0x5a, 0x59, 0x0c, 0x56, 0x03,
	0x00, 0x55, 0x55, 0x00, 0x03, 0x56, 0x0c, 0x59, 0x5a, 0x0f,
	0x0c, 0x59, 0x5a, 0x0f, 0x55, 0x00, 0x03, 0x56, 0x56, 0x03,
	0x00, 0x55, 0x0f, 0x5a, 0x59, 0x0c, 0x69, 0x3c, 0x3f, 0x6a,
	0x30, 0x65, 0x66, 0x33, 0x33, 0x66, 0x65, 0x30, 0x6a, 0x3f,
	0x3c, 0x69, 0x03, 0x56, 0x55, 0x00, 0x5a, 0x0f, 0x0c, 0x59,
	0x59, 0x0c, 0x0f, 0x5a, 0x00, 0x55, 0x56, 0x03, 0x66, 0x33,
	0x30, 0x65, 0x3f, 0x6a, 0x69, 0x3c, 0x3c, 0x69, 0x6a, 0x3f,
	0x65, 0x30, 0x33, 0x66, 0x65, 0x30, 0x33, 0x66, 0x3c, 0x69,
	0x6a, 0x3f, 0x3f, 0x6a, 0x69, 0x3c, 0x66, 0x33, 0x30, 0x65,
	0x00, 0x55, 0x56, 0x03, 0x59, 0x0c, 0x0f, 0x5a, 0x5a, 0x0f,
	0x0c, 0x59, 0x03, 0x56, 0x55, 0x00
};

int cavan_ecc_polarity_row(u8 data)
{
	int count;
	u8 shift;

	for (shift = 1, count = 0; shift; shift <<= 1) {
		if (data & shift) {
			count++;
		}
	}

	return count & 1;
}

u8 cavan_ecc_polarity_column(u8 data)
{
	unsigned int i;
	u8 cp;
	static const u8 cp_table[] = {
		0x55, 0xaa, 0x33, 0xCC, 0x0F, 0xF0
	};

	for (cp = 0, i = 0; i < NELEM(cp_table); i++) {
		if (cavan_ecc_polarity_row(data & cp_table[i])) {
			cp |= 1 << i;
		}
	}

	return cp;
}

void cavan_ecc_show_ecc_table(const u8 *table, size_t count)
{
	const u8 *table_end;

	for (table_end = table + count; table < table_end; table++) {
		print("0x%02x ", *table);
	}

	print_char('\n');
}

u8 *cavan_ecc_build_ecc_table(u8 *table, size_t count)
{
	unsigned int i;
	u8 ecc;

	for (i = 0; i < count; i++) {
		ecc = cavan_ecc_polarity_column(i);
		if (cavan_ecc_polarity_row(i)) {
			ecc |= 1 << 6;
		}

		table[i] = ecc;
	}

	return table;
}

static u16 cavan_ecc_check_row(u8 *buff)
{
	u16 ecc;
	u8 ecc_rows[256], *p, *p_end;

	for (p = ecc_rows, p_end = p + 256; p < p_end; p++, buff++) {
		*p =  cavan_ecc_table[*buff] & (1 << 6);
	}

	ecc = 0;

	return ecc;
}

static u8 cavan_ecc_check_column(u8 *buff)
{
	u8 ecc;
	u8 *buff_end;

	ecc = *buff;

	for (buff_end = buff + 256, buff++; buff < buff_end; buff++) {
		ecc ^= *buff;
	}

	return cavan_ecc_table[ecc] & 0x3F;
}

u32 cavan_ecc_check(void *buff, size_t size)
{
	if (size != 256) {
		pr_err_info("buff size != 256");
		return 0;
	}

	return cavan_ecc_check_row(buff) | cavan_ecc_check_column(buff) << (16 + 2) | 0x03 << 16;
}
