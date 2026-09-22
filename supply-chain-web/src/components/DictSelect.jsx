import { Select } from 'antd';
import { useDict } from '../hooks/useDict';

// 【核心修改】：使用 props 接收，然后在内部解构，防止参数漏写导致 ReferenceError
export default function DictSelect(props) {
    // 在这里解构，即使外部没传 value，这里解构出来也只是 undefined，绝不会报错
    const { dictType, placeholder, value, onChange, ...restProps } = props;

    const [dicts] = useDict(dictType);

    // 1. 将字典选项的 value 统一转为字符串
    const options = (dicts[dictType] || []).map(d => ({
        label: d.dictLabel,
        value: String(d.dictValue),
    }));

    // 2. 处理回显值：将数字或字符串统一转为字符串，以匹配 options 中的 value
    let safeValue = undefined;
    if (value !== undefined && value !== null && value !== '') {
        safeValue = String(value);
    }

    return (
        <Select
            options={options}
            placeholder={placeholder || '请选择'}
            value={safeValue}      // 使用转换后的安全值
            onChange={onChange}    // 显式传递 onChange
            {...restProps}         // 透传其他属性 (如 disabled, style 等)
        />
    );
}
