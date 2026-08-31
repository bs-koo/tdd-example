import { useState } from 'react';

export type DateSelectorProps = {
  date: string;
  onChange: (date: string) => void;
};

const COMPLETE_DATE = /^\d{4}-\d{2}-\d{2}$/;

// 화면에 하나뿐인 입력이라 고정 id 로 둔다. 페이지가 이 id 로 가시 라벨을 연결한다.
export const DATE_INPUT_ID = 'reservation-date';

export default function DateSelector({ date, onChange }: DateSelectorProps) {
  const [draft, setDraft] = useState(date);
  const [lastDate, setLastDate] = useState(date);

  if (date !== lastDate) {
    setLastDate(date);
    setDraft(date);
  }

  return (
    <input
      id={DATE_INPUT_ID}
      aria-label="날짜 선택"
      className="datebar__input"
      placeholder="YYYY-MM-DD"
      maxLength={10}
      autoComplete="off"
      value={draft}
      onChange={(e) => {
        const next = e.target.value;
        setDraft(next);
        if (COMPLETE_DATE.test(next)) {
          onChange(next);
        }
      }}
    />
  );
}
