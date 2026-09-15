import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'MindStep | Dementia risk support for Parkinson’s',
  description: 'A plain-language SMART-on-FHIR support prototype for people living with Parkinson’s disease.',
  openGraph: {
    title: 'MindStep | Small steps for better brain health',
    description: 'Clear, personalised guidance for people living with Parkinson’s disease.',
    type: 'website',
    images: [{ url: 'http://localhost:3000/og.png', width: 1200, height: 630, alt: 'MindStep — Small steps. Clear guidance. Better brain health.' }],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'MindStep | Small steps for better brain health',
    description: 'Clear, personalised guidance for people living with Parkinson’s disease.',
    images: ['http://localhost:3000/og.png'],
  },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>
        <script dangerouslySetInnerHTML={{ __html: "try{if(localStorage.getItem('mindstep-theme')==='dark'){document.documentElement.dataset.theme='dark'}}catch(e){}" }} />
        {children}
      </body>
    </html>
  );
}
