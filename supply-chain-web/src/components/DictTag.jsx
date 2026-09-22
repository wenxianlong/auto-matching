import { Tag } from 'antd';
import { useDict } from '../hooks/useDict';

export default function DictTag({ dictType, value }) {
    const [dicts] = useDict(dictType);
    const dictList = dicts[dictType] || [];

    // 兼容数字和字符串的比对
    const dict = dictList.find(d => String(d.dictValue) === String(value));

    if (!dict) return <span>{value}</span>;

    return <Tag color={dict.listClass || 'default'}>{dict.dictLabel}</Tag>;
}
