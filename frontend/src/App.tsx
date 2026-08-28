import ReservationPage from './pages/ReservationPage';
import { today } from './today';

export default function App() {
  return <ReservationPage initialDate={today()} />;
}
