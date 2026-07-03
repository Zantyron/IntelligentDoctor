export type ChatMode = "diagnosis" | "registration";

export type MessageRole = "user" | "assistant" | "system";

export interface ChatMessage {
  id?: string;
  role: MessageRole;
  content: string;
  streaming?: boolean;
}

export interface ChatSession {
  sessionId: string;
  hospitalId?: string;
  terminalUsername?: string;
  title?: string;
  mode?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Recommendation {
  type: string;
  title: string;
  description?: string;
  reason?: string;
}

export interface AppointmentOption {
  clinicName?: string;
  clinicLocation?: string;
  doctorName?: string;
  doctorTitle?: string;
  specialty?: string;
  slotDate?: string;
  period?: string;
  stockAvailable?: number | string;
  consultationFee?: number | string;
}

export interface RegistrationDraft {
  draftId: string;
  status?: string;
  departmentId?: string;
  doctorId?: string;
  visitDate?: string;
  visitPeriod?: string;
  patientName?: string;
  patientPhone?: string;
  idCard?: string;
  gender?: string;
  age?: number;
}

export interface ChatResultMetadata {
  recommendations?: Recommendation[];
  evidence?: string[];
  metadata?: {
    appointmentOptions?: AppointmentOption[];
    draft?: RegistrationDraft;
  };
}

export interface RegistrationOrder {
  orderNo: string;
  patientName: string;
  gender?: string;
  age?: number | string;
  visitDate?: string;
  visitPeriod?: string;
}

export interface ConfirmFormData {
  patientName: string;
  patientPhone: string;
  idCard: string;
  gender: string;
  age: number;
}

export type ToastType = "success" | "error" | "warning" | "info";

export interface ToastItem {
  id: number;
  message: string;
  type: ToastType;
}

export type RegistrationStep = "chat" | "draft" | "confirm" | "success";
